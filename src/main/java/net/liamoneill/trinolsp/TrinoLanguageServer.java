package net.liamoneill.trinolsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class TrinoLanguageServer implements LanguageServer, LanguageClientAware {
    private TextDocumentService textDocumentService;
    private WorkspaceService workspaceService;

    public TrinoLanguageServer() {
        this.textDocumentService = new TrinoTextDocumentService();
        this.workspaceService = new TrinoWorkspaceServer();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        // Initialize the InitializeResult for this LS.
        InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

        // Set the capabilities of the LS to inform the client.
        initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
        CompletionOptions completionOptions = new CompletionOptions();
        initializeResult.getCapabilities().setCompletionProvider(completionOptions);

        return CompletableFuture.supplyAsync(() -> initializeResult);
    }

    @Override
    public void connect(LanguageClient languageClient) {
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        // ignore
        return null;
    }

    @Override
    public void exit() {
        // ignore
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
}
