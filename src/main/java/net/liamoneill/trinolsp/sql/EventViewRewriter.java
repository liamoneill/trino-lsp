package net.liamoneill.trinolsp.sql;

import com.google.common.collect.Iterables;
import io.trino.sql.tree.*;
import net.liamoneill.trinolsp.TreeRewriter;

import java.util.*;
import java.util.stream.Collectors;

public class EventViewRewriter extends TreeRewriter<Void> {

    private EventViewRewriter() {
    }

    public static Node rewrite(Node node) {
        return new EventViewRewriter().process(node);
    }

    @Override
    protected Node visitQuery(Query query, Void context) {
        return new Query(
                query.getWith().map(w -> (With) process(w)),
                (QueryBody) process(query.getQueryBody()),
                query.getOrderBy(),
                query.getOffset(),
                query.getLimit());
    }

    @Override
    protected Node visitWith(With with, Void context) {
        return new With(
                with.isRecursive(),
                with.getQueries().stream()
                        .map(q -> (WithQuery) process(q))
                        .collect(Collectors.toList())
        );
    }

    @Override
    protected Node visitWithQuery(WithQuery withQuery, Void context) {
        return new WithQuery(
                withQuery.getName(),
                (Query) process(withQuery.getQuery()),
                withQuery.getColumnNames()
        );
    }

    @Override
    protected Node visitQuerySpecification(QuerySpecification node, Void context) {
        if (node.getFrom().isEmpty()) {
            return node;
        }

        Context queryRelationContext = new Context();

        if (node.getWhere().isPresent()) {
            expressionColumnConstraints(node.getWhere().get(), queryRelationContext);
        }

        return new QuerySpecification(
                node.getSelect(),
                node.getFrom().map(from -> rewriteRelation(from, queryRelationContext)),
                node.getWhere().map(where -> (Expression) process(where)),
                node.getGroupBy(),
                node.getHaving(),
                node.getWindows(),
                node.getOrderBy(),
                node.getOffset(),
                node.getLimit()
        );
    }

    @Override
    protected Node visitInPredicate(InPredicate node, Void context) {
        return new InPredicate(
                node.getValue(),
                (Expression) process(node.getValueList())
        );
    }

    @Override
    protected Node visitSubqueryExpression(SubqueryExpression node, Void context) {
        return new SubqueryExpression(
                (Query) process(node.getQuery())
        );
    }

    private Relation rewriteRelation(Relation relation, Context context) {
        if (relation instanceof AliasedRelation) {
            AliasedRelation aliasRelation = (AliasedRelation) relation;
            String alias = aliasRelation.getAlias().getValue();
            Relation aliasedRelation = aliasRelation.getRelation();

            if (aliasedRelation instanceof Table) {
                Table table = (Table) aliasedRelation;
                return new AliasedRelation(
                        rewriteRelation(table, context.getColumnConstraints(alias)),
                        aliasRelation.getAlias(),
                        aliasRelation.getColumnNames()
                );
            }
        } else if (relation instanceof Join) {
            Join join = (Join) relation;
            return new Join(
                    join.getType(),
                    rewriteRelation(join.getLeft(), context),
                    rewriteRelation(join.getRight(), context),
                    join.getCriteria()
            );
        }

        return relation;
    }

    private Table rewriteRelation(Table table, List<Context.ColumnConstraint> columnConstraints) {
        if (!table.getName().getSuffix().equals("events")) {
            return table;
        }

        Set<String> columns = columnConstraints.stream()
                .filter(columnConstraint ->
                        columnConstraint.constraintType == Context.ColumnConstraintType.EQUAL ||
                                columnConstraint.constraintType ==  Context.ColumnConstraintType.IN)
                .map(c -> c.column)
                .collect(Collectors.toSet());

        // TODO handle qualified names
        List<String> nameParts = new ArrayList<>(table.getName().getParts());
        String tableName = Iterables.getLast(nameParts);
        if (columns.contains("id")) {
            tableName = "events_by_id";
        } else if (columns.contains("session_id")) {
            tableName = "events_by_session_id";
        } else if (columns.contains("profile_id") || columns.contains("sid")) {
            tableName = "events_by_profile_id";
        } else if (columns.contains("type")) {
            tableName = "events_by_type";
        }

        nameParts.set(nameParts.size() - 1, tableName);
        QualifiedName qualifiedName = QualifiedName.of(nameParts.stream().map(Identifier::new).collect(Collectors.toList()));
        return new Table(qualifiedName);
    }

    private void expressionColumnConstraints(Expression expr, EventViewRewriter.Context context) {
        if (expr instanceof ComparisonExpression) {
            expressionColumnConstraints((ComparisonExpression) expr, context);
        } else if (expr instanceof LogicalBinaryExpression) {
            expressionColumnConstraints((LogicalBinaryExpression) expr, context);
        } else if (expr instanceof InPredicate) {
            expressionColumnConstraints((InPredicate) expr, context);
        }
    }

    private void expressionColumnConstraints(ComparisonExpression expr, EventViewRewriter.Context context) {
        if (expr.getOperator() == ComparisonExpression.Operator.EQUAL) {
            if (expr.getLeft() instanceof DereferenceExpression &&
                expr.getRight() instanceof StringLiteral) {
                // a.b = 'literal'
                expressionColumnEqualConstraint(
                        (DereferenceExpression) expr.getLeft(),
                        (StringLiteral) expr.getRight(),
                        context);
            } else if (expr.getLeft() instanceof StringLiteral &&
                        expr.getRight() instanceof DereferenceExpression) {
                // 'literal' = a.b
                expressionColumnEqualConstraint(
                        (DereferenceExpression) expr.getRight(),
                        (StringLiteral) expr.getLeft(),
                        context);
            }
        }
    }

    private void expressionColumnConstraints(LogicalBinaryExpression expr, EventViewRewriter.Context context) {
        if (expr.getOperator() == LogicalBinaryExpression.Operator.AND) {
            expressionColumnConstraints(expr.getLeft(), context);
            expressionColumnConstraints(expr.getRight(), context);
        }
    }

    private void expressionColumnConstraints(InPredicate expr, EventViewRewriter.Context context) {
        if (expr.getValue() instanceof DereferenceExpression && expr.getValueList() instanceof InListExpression) {
            DereferenceExpression left = (DereferenceExpression) expr.getValue();
            InListExpression right = (InListExpression) expr.getValueList();

            if (left.getBase() instanceof Identifier) {
                Identifier base = (Identifier) left.getBase();

                List<String> inValues = right.getValues().stream()
                        .filter(e -> e instanceof StringLiteral)
                        .map(e -> ((StringLiteral)e ).getValue())
                        .collect(Collectors.toList());

                context.addColumnConstraint(
                        base.getValue(),
                        new Context.ColumnConstraint(
                            Context.ColumnConstraintType.IN,
                            left.getField().getValue(),
                            inValues));
            }
        }
    }

    private void expressionColumnEqualConstraint(DereferenceExpression left, StringLiteral right, EventViewRewriter.Context context) {
        if (left.getBase() instanceof Identifier) {
            Identifier base = (Identifier) left.getBase();

            context.addColumnConstraint(
                    base.getValue(),
                    new Context.ColumnConstraint(
                        Context.ColumnConstraintType.EQUAL,
                        left.getField().getValue(),
                        right.getValue()));
        }
    }

    public static class Context {
        private final Map<String, List<ColumnConstraint>> columnConstraints = new HashMap<>();

        public void addColumnConstraint(String tableName, ColumnConstraint columnConstraint) {
            columnConstraints.compute(tableName, (t, columns) -> {
                columns = columns == null ? new ArrayList<>() : columns;
                columns.add(columnConstraint);
                return columns;
            });
        }

        public List<ColumnConstraint> getColumnConstraints(String tableName) {
            return columnConstraints.getOrDefault(tableName, Collections.emptyList());
        }

        public static class ColumnConstraint {
            public final ColumnConstraintType constraintType;
            public final String column;
            public final Object value;

            public ColumnConstraint(ColumnConstraintType constraintType, String column, Object value) {
                this.constraintType = constraintType;
                this.column = column;
                this.value = value;
            }
        }

        public enum ColumnConstraintType {
            EQUAL,
            IN
        }
    }
}
