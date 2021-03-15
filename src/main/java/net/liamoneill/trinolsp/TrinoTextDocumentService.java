package net.liamoneill.trinolsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TrinoTextDocumentService implements TextDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrinoTextDocumentService.class);

    private final TrinoLanguageServer trinoLanguageServer;
    private final Map<String, TextDocumentItem> openedDocuments = new HashMap<>();

    public TrinoTextDocumentService(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        String uri = completionParams.getTextDocument().getUri();
        LOGGER.info("completion: {}", uri);

        return new CompletionRunner(trinoLanguageServer).compute(completionParams);
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        LOGGER.info("resolveCompletionItem: {}", unresolved.getLabel());
        return CompletableFuture.completedFuture(unresolved);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        LOGGER.info("hover: {}", params.getTextDocument());

        return new HoverRunner(trinoLanguageServer).compute(params);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        LOGGER.info("signatureHelp: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        LOGGER.info("definition: {}", textDocument);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        LOGGER.info("references: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        LOGGER.info("documentHighlight: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        LOGGER.info("documentSymbol: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        LOGGER.info("codeAction: {}", params.getTextDocument());

        return new CodeActionsRunner(trinoLanguageServer).compute(params);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        LOGGER.info("codeLens: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        LOGGER.info("resolveCodeLens: {}", unresolved.getCommand().getCommand());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        LOGGER.info("formatting: {}", params.getTextDocument());


        return new FormatterRunner(trinoLanguageServer).compute(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        LOGGER.info("rangeFormatting: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        LOGGER.info("onTypeFormatting: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        LOGGER.info("rename: {}", params.getTextDocument());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem textDocument = params.getTextDocument();
        LOGGER.info("didOpen: {}", textDocument);
        openedDocuments.put(textDocument.getUri(), textDocument);

        new DiagnosticRunner(trinoLanguageServer).compute(params);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        LOGGER.info("didChange: {}", params.getTextDocument());
        List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
        TextDocumentItem textDocumentItem = openedDocuments.get(params.getTextDocument().getUri());
        if (!contentChanges.isEmpty()) {
            textDocumentItem.setText(contentChanges.get(0).getText());
            new DiagnosticRunner(trinoLanguageServer).compute(params);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        LOGGER.info("didClose: {}", params.getTextDocument());
        String uri = params.getTextDocument().getUri();
        openedDocuments.remove(uri);

        /* The rule observed by VS Code servers as explained in LSP specification is to clear the Diagnostic when it is related to a single file.
         * https://microsoft.github.io/language-server-protocol/specification#textDocument_publishDiagnostics
         * */
        new DiagnosticRunner(trinoLanguageServer).clear(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        LOGGER.info("didSave: {}", params.getTextDocument());
        new DiagnosticRunner(trinoLanguageServer).compute(params);
    }

    public TextDocumentItem getOpenedDocument(String uri) {
        return openedDocuments.get(uri);
    }

    public Collection<TextDocumentItem> getAllOpenedDocuments() {
        return openedDocuments.values();
    }
}
