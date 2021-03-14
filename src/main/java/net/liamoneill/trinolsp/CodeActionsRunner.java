package net.liamoneill.trinolsp;

import net.liamoneill.trinolsp.sql.Formatter;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CodeActionsRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeActionsRunner.class);

    private final TrinoLanguageServer trinoLanguageServer;

    public CodeActionsRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public CompletableFuture<List<Either<Command, CodeAction>>> compute(CodeActionParams params) {
        String documentUri = params.getTextDocument().getUri();
        String text = trinoLanguageServer.getTextDocumentService()
                .getOpenedDocument(documentUri)
                .getText();

        LOGGER.info("Computing code actions for {}", params.getContext().getDiagnostics());

        List<Either<Command, CodeAction>> codeActions = new ArrayList<>();

        for (Diagnostic diagnostics : params.getContext().getDiagnostics()) {
            if (!Objects.equals(diagnostics.getMessage(), "Selecting all results from a large table")) {
                continue;
            }

            List<TextEdit> edits = Utils.editsForDiff(text, text.stripTrailing() + " LIMIT 100");
            Map<String, List<TextEdit>> workspaceEdits = Collections.singletonMap(documentUri, edits);
            CodeAction codeAction = new CodeAction("Limit the query to 100 results");
            codeAction.setEdit(new WorkspaceEdit(workspaceEdits));
            codeActions.add(Either.forRight(codeAction));
        }

        return CompletableFuture.completedFuture(codeActions);
    }
}
