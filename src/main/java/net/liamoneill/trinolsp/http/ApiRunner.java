package net.liamoneill.trinolsp.http;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.Service;

public class ApiRunner {
    private static final String DEFAULT_HOSTNAME = "127.0.0.1";
    private static final int DEFAULT_PORT = 5001;

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final String hostname;
    private final int port;

    public ApiRunner(String hostname, Integer port) {
        this.hostname = hostname == null ? DEFAULT_HOSTNAME : hostname;
        this.port = port == null ? DEFAULT_PORT : port;
    }

    public void run() {
        Service http = Service.ignite()
                .ipAddress(hostname)
                .port(port);

        http.post("/v1/rewrite", (req, res) -> {
            return new RewriteEndpoint().handle(GSON.fromJson(req.body(), RewriteEndpoint.Request.class));
        }, GSON::toJson);
    }
}
