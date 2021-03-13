package net.liamoneill.trinolsp.websocket;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.Set;

public class MyLSPWebSocketServerConfigProvider implements ServerApplicationConfig {

    public static final String WEBSOCKET_SERVER_PATH = "/my-language-server";

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        ServerEndpointConfig conf = ServerEndpointConfig.Builder.create(MyLSPWebSocketEndpoint.class, WEBSOCKET_SERVER_PATH).build();
        return Collections.singleton(conf);
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return scanned;
    }

}