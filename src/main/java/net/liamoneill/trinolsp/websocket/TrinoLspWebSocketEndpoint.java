package net.liamoneill.trinolsp.websocket;

import java.util.Collection;

import net.liamoneill.trinolsp.TrinoLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.websocket.WebSocketEndpoint;

public class TrinoLspWebSocketEndpoint extends WebSocketEndpoint<LanguageClient> {

    @Override
    protected void configure(Builder<LanguageClient> builder) {
        builder.setLocalService(new TrinoLanguageServer());
        builder.setRemoteInterface(LanguageClient.class);
    }

    @Override
    protected void connect(Collection<Object> localServices, LanguageClient remoteProxy) {
        localServices.stream()
                .filter(LanguageClientAware.class::isInstance)
                .forEach(languageClientAware -> ((LanguageClientAware) languageClientAware).connect(remoteProxy));
    }
}
