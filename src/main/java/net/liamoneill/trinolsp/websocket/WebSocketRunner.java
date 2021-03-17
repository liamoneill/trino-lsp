package net.liamoneill.trinolsp.websocket;

import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;

public class WebSocketRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketRunner.class);

    private static final String DEFAULT_HOSTNAME = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_CONTEXT_PATH = "/";

    public void runWebSocketServer(String hostname, Integer port, String contextPath) {
        hostname = hostname != null ? hostname : DEFAULT_HOSTNAME;
        port = port != null ? port : DEFAULT_PORT;
        contextPath = contextPath != null ? contextPath : DEFAULT_CONTEXT_PATH;
        Server server = new Server(hostname, port, contextPath, null, TrinoLspWebSocketServerConfigProvider.class);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "trino-lsp-websocket-server-shutdown-hook"));

        try {
            server.start();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            LOGGER.error("Trino LSP Websocket server has been interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (DeploymentException e) {
            LOGGER.error("Cannot start Trino LSP Websocket server.", e);
        } finally {
            server.stop();
        }
    }
}