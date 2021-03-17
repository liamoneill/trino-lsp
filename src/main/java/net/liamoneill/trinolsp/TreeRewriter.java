package net.liamoneill.trinolsp;

import io.trino.sql.tree.*;

public abstract class TreeRewriter<C> extends AstVisitor<Node, C>
{
    @Override
    protected Node visitExtract(Extract node, C context)
    {
        process(node.getExpression(), context);
        return node;
    }

    @Override
    protected Node visitCast(Cast node, C context)
    {
        process(node.getExpression(), context);
        return node;
    }

    @Override
    protected Node visitArithmeticBinary(ArithmeticBinaryExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return node;
    }

    @Override
    protected Node visitBetweenPredicate(BetweenPredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getMin(), context);
        process(node.getMax(), context);

        return node;
    }

    @Override
    protected Node visitCoalesceExpression(CoalesceExpression node, C context)
    {
        for (Expression operand : node.getOperands()) {
            process(operand, context);
        }

        return node;
    }

    @Override
    protected Node visitAtTimeZone(AtTimeZone node, C context)
    {
        process(node.getValue(), context);
        process(node.getTimeZone(), context);

        return node;
    }

    @Override
    protected Node visitArrayConstructor(ArrayConstructor node, C context)
    {
        for (Expression expression : node.getValues()) {
            process(expression, context);
        }

        return node;
    }

    @Override
    protected Node visitSubscriptExpression(SubscriptExpression node, C context)
    {
        process(node.getBase(), context);
        process(node.getIndex(), context);

        return node;
    }

    @Override
    protected Node visitComparisonExpression(ComparisonExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return node;
    }

    @Override
    protected Node visitFormat(Format node, C context)
    {
        for (Expression argument : node.getArguments()) {
            process(argument, context);
        }

        return node;
    }

    @Override
    protected Node visitQuery(Query node, C context)
    {
        if (node.getWith().isPresent()) {
            process(node.getWith().get(), context);
        }
        process(node.getQueryBody(), context);
        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }
        if (node.getOffset().isPresent()) {
            process(node.getOffset().get(), context);
        }
        if (node.getLimit().isPresent()) {
            process(node.getLimit().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitWith(With node, C context)
    {
        for (WithQuery query : node.getQueries()) {
            process(query, context);
        }

        return node;
    }

    @Override
    protected Node visitWithQuery(WithQuery node, C context)
    {
        process(node.getQuery(), context);
        return node;
    }

    @Override
    protected Node visitSelect(Select node, C context)
    {
        for (SelectItem item : node.getSelectItems()) {
            process(item, context);
        }

        return node;
    }

    @Override
    protected Node visitSingleColumn(SingleColumn node, C context)
    {
        process(node.getExpression(), context);

        return node;
    }

    @Override
    protected Node visitAllColumns(AllColumns node, C context)
    {
        node.getTarget().ifPresent(value -> process(value, context));

        return node;
    }

    @Override
    protected Node visitWhenClause(WhenClause node, C context)
    {
        process(node.getOperand(), context);
        process(node.getResult(), context);

        return node;
    }

    @Override
    protected Node visitInPredicate(InPredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getValueList(), context);

        return node;
    }

    @Override
    protected Node visitFunctionCall(FunctionCall node, C context)
    {
        for (Expression argument : node.getArguments()) {
            process(argument, context);
        }

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }

        if (node.getWindow().isPresent()) {
            process((Node) node.getWindow().get(), context);
        }

        if (node.getFilter().isPresent()) {
            process(node.getFilter().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitGroupingOperation(GroupingOperation node, C context)
    {
        for (Expression columnArgument : node.getGroupingColumns()) {
            process(columnArgument, context);
        }

        return node;
    }

    @Override
    protected Node visitDereferenceExpression(DereferenceExpression node, C context)
    {
        process(node.getBase(), context);
        return node;
    }

    @Override
    protected Node visitWindowReference(WindowReference node, C context)
    {
        process(node.getName(), context);

        return node;
    }

    @Override
    protected Node visitWindowSpecification(WindowSpecification node, C context)
    {
        if (node.getExistingWindowName().isPresent()) {
            process(node.getExistingWindowName().get(), context);
        }

        for (Expression expression : node.getPartitionBy()) {
            process(expression, context);
        }

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }

        if (node.getFrame().isPresent()) {
            process(node.getFrame().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitWindowDefinition(WindowDefinition node, C context)
    {
        process(node.getWindow());

        return node;
    }

    @Override
    protected Node visitWindowFrame(WindowFrame node, C context)
    {
        process(node.getStart(), context);
        if (node.getEnd().isPresent()) {
            process(node.getEnd().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitFrameBound(FrameBound node, C context)
    {
        if (node.getValue().isPresent()) {
            process(node.getValue().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitOffset(Offset node, C context)
    {
        process(node.getRowCount());

        return node;
    }

    @Override
    protected Node visitLimit(Limit node, C context)
    {
        process(node.getRowCount());

        return node;
    }

    @Override
    protected Node visitFetchFirst(FetchFirst node, C context)
    {
        node.getRowCount().ifPresent(this::process);

        return node;
    }

    @Override
    protected Node visitSimpleCaseExpression(SimpleCaseExpression node, C context)
    {
        process(node.getOperand(), context);
        for (WhenClause clause : node.getWhenClauses()) {
            process(clause, context);
        }

        node.getDefaultValue()
                .ifPresent(value -> process(value, context));

        return node;
    }

    @Override
    protected Node visitInListExpression(InListExpression node, C context)
    {
        for (Expression value : node.getValues()) {
            process(value, context);
        }

        return node;
    }

    @Override
    protected Node visitNullIfExpression(NullIfExpression node, C context)
    {
        process(node.getFirst(), context);
        process(node.getSecond(), context);

        return node;
    }

    @Override
    protected Node visitIfExpression(IfExpression node, C context)
    {
        process(node.getCondition(), context);
        process(node.getTrueValue(), context);
        if (node.getFalseValue().isPresent()) {
            process(node.getFalseValue().get(), context);
        }

        return node;
    }

    @Override
    protected Node visitTryExpression(TryExpression node, C context)
    {
        process(node.getInnerExpression(), context);
        return node;
    }

    @Override
    protected Node visitBindExpression(BindExpression node, C context)
    {
        for (Expression value : node.getValues()) {
            process(value, context);
        }
        process(node.getFunction(), context);

        return node;
    }

    @Override
    protected Node visitArithmeticUnary(ArithmeticUnaryExpression node, C context)
    {
        process(node.getValue(), context);
        return node;
    }

    @Override
    protected Node visitNotExpression(NotExpression node, C context)
    {
        process(node.getValue(), context);
        return node;
    }

    @Override
    protected Node visitSearchedCaseExpression(SearchedCaseExpression node, C context)
    {
        for (WhenClause clause : node.getWhenClauses()) {
            process(clause, context);
        }
        node.getDefaultValue()
                .ifPresent(value -> process(value, context));

        return node;
    }

    @Override
    protected Node visitLikePredicate(LikePredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getPattern(), context);
        node.getEscape().ifPresent(value -> process(value, context));

        return node;
    }

    @Override
    protected Node visitIsNotNullPredicate(IsNotNullPredicate node, C context)
    {
        process(node.getValue(), context);
        return node;
    }

    @Override
    protected Node visitIsNullPredicate(IsNullPredicate node, C context)
    {
        process(node.getValue(), context);
        return node;
    }

    @Override
    protected Node visitLogicalBinaryExpression(LogicalBinaryExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return node;
    }

    @Override
    protected Node visitSubqueryExpression(SubqueryExpression node, C context)
    {
        process(node.getQuery(), context);
        return node;
    }

    @Override
    protected Node visitOrderBy(OrderBy node, C context)
    {
        for (SortItem sortItem : node.getSortItems()) {
            process(sortItem, context);
        }
        return node;
    }

    @Override
    protected Node visitSortItem(SortItem node, C context)
    {
        process(node.getSortKey(), context);
        return node;
    }

    @Override
    protected Node visitQuerySpecification(QuerySpecification node, C context)
    {
        process(node.getSelect(), context);
        if (node.getFrom().isPresent()) {
            process(node.getFrom().get(), context);
        }
        if (node.getWhere().isPresent()) {
            process(node.getWhere().get(), context);
        }
        if (node.getGroupBy().isPresent()) {
            process(node.getGroupBy().get(), context);
        }
        if (node.getHaving().isPresent()) {
            process(node.getHaving().get(), context);
        }
        for (WindowDefinition windowDefinition : node.getWindows()) {
            process(windowDefinition, context);
        }
        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }
        if (node.getOffset().isPresent()) {
            process(node.getOffset().get(), context);
        }
        if (node.getLimit().isPresent()) {
            process(node.getLimit().get(), context);
        }
        return node;
    }

    @Override
    protected Node visitSetOperation(SetOperation node, C context)
    {
        for (Relation relation : node.getRelations()) {
            process(relation, context);
        }
        return node;
    }

    @Override
    protected Node visitValues(Values node, C context)
    {
        for (Expression row : node.getRows()) {
            process(row, context);
        }
        return node;
    }

    @Override
    protected Node visitRow(Row node, C context)
    {
        for (Expression expression : node.getItems()) {
            process(expression, context);
        }
        return node;
    }

    @Override
    protected Node visitTableSubquery(TableSubquery node, C context)
    {
        process(node.getQuery(), context);
        return node;
    }

    @Override
    protected Node visitAliasedRelation(AliasedRelation node, C context)
    {
        process(node.getRelation(), context);
        return node;
    }

    @Override
    protected Node visitSampledRelation(SampledRelation node, C context)
    {
        process(node.getRelation(), context);
        process(node.getSamplePercentage(), context);
        return node;
    }

    @Override
    protected Node visitJoin(Join node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        node.getCriteria()
                .filter(criteria -> criteria instanceof JoinOn)
                .ifPresent(criteria -> process(((JoinOn) criteria).getExpression(), context));

        return node;
    }

    @Override
    protected Node visitUnnest(Unnest node, C context)
    {
        for (Expression expression : node.getExpressions()) {
            process(expression, context);
        }

        return node;
    }

    @Override
    protected Node visitGroupBy(GroupBy node, C context)
    {
        for (GroupingElement groupingElement : node.getGroupingElements()) {
            process(groupingElement, context);
        }

        return node;
    }

    @Override
    protected Node visitCube(Cube node, C context)
    {
        return node;
    }

    @Override
    protected Node visitRollup(Rollup node, C context)
    {
        return node;
    }

    @Override
    protected Node visitSimpleGroupBy(SimpleGroupBy node, C context)
    {
        for (Expression expression : node.getExpressions()) {
            process(expression, context);
        }

        return node;
    }

    @Override
    protected Node visitGroupingSets(GroupingSets node, C context)
    {
        return node;
    }

    @Override
    protected Node visitInsert(Insert node, C context)
    {
        process(node.getQuery(), context);

        return node;
    }

    @Override
    protected Node visitRefreshMaterializedView(RefreshMaterializedView node, C context)
    {
        return node;
    }

    @Override
    protected Node visitDelete(Delete node, C context)
    {
        process(node.getTable(), context);
        node.getWhere().ifPresent(where -> process(where, context));

        return node;
    }

    @Override
    protected Node visitUpdate(Update node, C context)
    {
        process(node.getTable(), context);
        node.getAssignments().forEach(value -> process(value, context));
        node.getWhere().ifPresent(where -> process(where, context));

        return node;
    }

    @Override
    protected Node visitUpdateAssignment(UpdateAssignment node, C context)
    {
        process(node.getName(), context);
        process(node.getValue(), context);
        return node;
    }

    @Override
    protected Node visitCreateTableAsSelect(CreateTableAsSelect node, C context)
    {
        process(node.getQuery(), context);
        for (Property property : node.getProperties()) {
            process(property, context);
        }

        return node;
    }

    @Override
    protected Node visitProperty(Property node, C context)
    {
        process(node.getName(), context);
        process(node.getValue(), context);

        return node;
    }

    @Override
    protected Node visitAnalyze(Analyze node, C context)
    {
        for (Property property : node.getProperties()) {
            process(property, context);
        }
        return node;
    }

    @Override
    protected Node visitCreateView(CreateView node, C context)
    {
        process(node.getQuery(), context);

        return node;
    }

    @Override
    protected Node visitSetSession(SetSession node, C context)
    {
        process(node.getValue(), context);

        return node;
    }

    @Override
    protected Node visitAddColumn(AddColumn node, C context)
    {
        process(node.getColumn(), context);

        return node;
    }

    @Override
    protected Node visitCreateTable(CreateTable node, C context)
    {
        for (TableElement tableElement : node.getElements()) {
            process(tableElement, context);
        }
        for (Property property : node.getProperties()) {
            process(property, context);
        }

        return node;
    }

    @Override
    protected Node visitStartTransaction(StartTransaction node, C context)
    {
        for (TransactionMode transactionMode : node.getTransactionModes()) {
            process(transactionMode, context);
        }

        return node;
    }

    @Override
    protected Node visitExplain(Explain node, C context)
    {
        process(node.getStatement(), context);

        for (ExplainOption option : node.getOptions()) {
            process(option, context);
        }

        return node;
    }

    @Override
    protected Node visitShowStats(ShowStats node, C context)
    {
        process(node.getRelation(), context);
        return node;
    }

    @Override
    protected Node visitQuantifiedComparisonExpression(QuantifiedComparisonExpression node, C context)
    {
        process(node.getValue(), context);
        process(node.getSubquery(), context);

        return node;
    }

    @Override
    protected Node visitExists(ExistsPredicate node, C context)
    {
        process(node.getSubquery(), context);

        return node;
    }

    @Override
    protected Node visitLateral(Lateral node, C context)
    {
        process(node.getQuery(), context);

        return node;
    }

    @Override
    protected Node visitLambdaExpression(LambdaExpression node, C context)
    {
        process(node.getBody(), context);

        return node;
    }
}
