package net.liamoneill.trinolsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class TrinoLanguageServer implements LanguageServer, LanguageClientAware {

    private final TrinoTextDocumentService textDocumentService;
    private final TrinoWorkspaceService workspaceService;

    private LanguageClient client;

    public TrinoLanguageServer() {
        this.textDocumentService = new TrinoTextDocumentService(this);
        this.workspaceService = new TrinoWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

        ServerCapabilities capabilities = initializeResult.getCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.setCodeLensProvider(new CodeLensOptions());
        capabilities.setDocumentFormattingProvider(new DocumentFormattingOptions());
        capabilities.setCodeActionProvider(new CodeActionOptions(Collections.singletonList(CodeActionKind.QuickFix)));

        return CompletableFuture.supplyAsync(() -> initializeResult);
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
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
    public TrinoTextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    public LanguageClient getClient() {
        return client;
    }
}
