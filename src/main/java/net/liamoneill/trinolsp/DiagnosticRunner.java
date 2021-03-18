package net.liamoneill.trinolsp;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.*;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiagnosticRunner {
    private final TrinoLanguageServer trinoLanguageServer;

    public DiagnosticRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public void compute(DidSaveTextDocumentParams params) {
        String text = retrieveFullText(params);
        computeDiagnostics(text, trinoLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
    }

    public void compute(DidChangeTextDocumentParams params) {
        String text = params.getContentChanges().get(0).getText();
        computeDiagnostics(text, trinoLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
    }

    public void compute(DidOpenTextDocumentParams params) {
        String text = params.getTextDocument().getText();
        computeDiagnostics(text, params.getTextDocument());
    }

    public void computeDiagnostics(String sql, TextDocumentItem documentItem) {
        String uri = documentItem.getUri();
        CompletableFuture.runAsync(() -> {
            Either<Statement, ParsingException> parseResult = Parser.parse(sql);
            List<Diagnostic> diagnostics = Collections.emptyList();

            if (parseResult.isLeft()) {
                Statement statement = parseResult.getLeft();

                if (statement instanceof Query && ((Query) statement).getQueryBody() instanceof QuerySpecification) {
                    QuerySpecification querySpecification = (QuerySpecification) ((Query) statement).getQueryBody();

                    if (querySpecification.getWhere().isEmpty()
                            && querySpecification.getLimit().isEmpty()
                            && querySpecification.getFrom().isPresent()) {

                        Relation relation = querySpecification.getFrom().get();
                        if ((relation instanceof Table) && isLargeTable((Table) relation)) {
                            Position start = new Position(0, 0);
                            Position end = Utils.positionAt(sql, sql.length());
                            Range range = new Range(start, end);

                            Diagnostic diagnostic = new Diagnostic(range,
                                    "Selecting all results from a large table",
                                    DiagnosticSeverity.Warning,
                                    "Query Engine");
                            diagnostic.setData(1);
                            diagnostics = Collections.singletonList(diagnostic);
                        }
                    }
                }
            } else {
                ParsingException error = parseResult.getRight();
                Position errorPosition = new Position(error.getLineNumber() - 1, error.getColumnNumber());
                Range errorRange = new Range(errorPosition, errorPosition);

                diagnostics = Collections.singletonList(new Diagnostic(errorRange, error.getErrorMessage()));
            }

            trinoLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        });
    }

    private static String getTableName(Table table) {
        List<Identifier> nameParts = table.getName().getOriginalParts();
        return nameParts.get(nameParts.size() - 1).getValue();
    }

    private static boolean isLargeTable(Table table) {
        return Objects.equals(getTableName(table), "events");
    }

    private String retrieveFullText(DidSaveTextDocumentParams params) {
        String text = params.getText();
        if (text == null) {
            text = trinoLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()).getText();
        }
        return text;
    }

    public void clear(String uri) {
        trinoLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
    }
}
