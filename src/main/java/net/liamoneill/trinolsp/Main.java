package net.liamoneill.trinolsp;

import net.liamoneill.trinolsp.http.ApiRunner;
import net.liamoneill.trinolsp.websocket.WebSocketRunner;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String WEBSOCKET_PARAMETER = "--websocket";
    private static final String WEBSOCKET_HOSTNAME_PARAMETER = "--websocketHostname=";
    private static final String WEBSOCKET_PORT_PARAMETER = "--websocketPort=";
    private static final String WEBSOCKET_CONTEXTPATH_PARAMETER = "--websocketContextPath=";

    private static final String HTTP_PARAMETER = "--http";
    private static final String HTTP_HOSTNAME_PARAMETER = "--httpHostname=";
    private static final String HTTP_PORT_PARAMETER = "--httpPort=";

    public static void main(String[] args) {
        List<String> arguments = Arrays.asList(args);

        if (arguments.contains(HTTP_PARAMETER)) {
            String hostname = extractParameterValue(arguments, HTTP_HOSTNAME_PARAMETER);
            int port = extractPort(arguments, HTTP_PORT_PARAMETER);
            new ApiRunner(hostname, port).run();
        }

        if (arguments.contains(WEBSOCKET_PARAMETER)) {
            String hostname = extractParameterValue(arguments, WEBSOCKET_HOSTNAME_PARAMETER);
            int port = extractPort(arguments, WEBSOCKET_PORT_PARAMETER);
            String contextPath = extractParameterValue(arguments, WEBSOCKET_CONTEXTPATH_PARAMETER);
            new WebSocketRunner().runWebSocketServer(hostname, port, contextPath);
        } else {
            TrinoLanguageServer server = new TrinoLanguageServer();
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
            server.connect(launcher.getRemoteProxy());
            launcher.startListening();
        }
    }

    private static String extractParameterValue(List<String> arguments, String parameterToExtract) {
        for (String argument : arguments) {
            if (argument.startsWith(parameterToExtract)) {
                return argument.substring(parameterToExtract.length());
            }
        }
        return null;
    }

    private static int extractPort(List<String> arguments, String argumentName) {
        for (String argument : arguments) {
            if (argument.startsWith(argumentName)) {
                String providedPort = argument.substring(argumentName.length());
                try {
                    return Integer.parseInt(providedPort);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("The provided port is invalid.", nfe);
                }
            }
        }
        return -1;
    }
}
