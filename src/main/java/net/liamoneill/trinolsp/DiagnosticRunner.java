package net.liamoneill.trinolsp;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.Statement;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiagnosticRunner {
    private final TrinoLanguageServer trinoLanguageServer;

    public DiagnosticRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public void compute(DidSaveTextDocumentParams params) {
        String camelText = retrieveFullText(params);
        computeDiagnostics(camelText, trinoLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
    }

    public void compute(DidChangeTextDocumentParams params) {
        String camelText = params.getContentChanges().get(0).getText();

        computeDiagnostics(camelText, trinoLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
    }

    public void compute(DidOpenTextDocumentParams params) {
        String camelText = params.getTextDocument().getText();
        computeDiagnostics(camelText, params.getTextDocument());
    }

    public void computeDiagnostics(String sql, TextDocumentItem documentItem) {
        String uri = documentItem.getUri();
        CompletableFuture.runAsync(() -> {
            Either<Statement, ParsingException> parseResult = Parser.parse(sql);
            List<Diagnostic> diagnostics = Collections.emptyList();

            if (parseResult.isRight()) {
                ParsingException error = parseResult.getRight();
                Position errorPosition = new Position(error.getLineNumber() - 1, error.getColumnNumber());
                Range errorRange = new Range(errorPosition, errorPosition);

                diagnostics = Collections.singletonList(new Diagnostic(errorRange, error.getErrorMessage()));
            }

            trinoLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        });
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
