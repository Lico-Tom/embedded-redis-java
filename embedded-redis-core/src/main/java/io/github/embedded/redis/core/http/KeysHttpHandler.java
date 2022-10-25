package io.github.embedded.redis.core.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.embedded.redis.core.RedisEngine;
import io.github.embedded.redis.core.util.JacksonService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeysHttpHandler implements HttpHandler {

    private final RedisEngine redisEngine;

    public KeysHttpHandler(RedisEngine redisEngine) {
        this.redisEngine = redisEngine;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        switch (requestMethod) {
            case "GET" -> {
                InputStream is = exchange.getRequestBody();
                byte[] bytes = is.readAllBytes();
                TimeIntervalModule module = JacksonService.toPojo(bytes, TimeIntervalModule.class);
                if (Optional.ofNullable(module).isEmpty()) {
                    List<String> keys = redisEngine.keys("*");
                    String s = String.join(",", keys);
                    exchange.sendResponseHeaders(200, s.length());
                    exchange.getResponseBody().write(s.getBytes(StandardCharsets.UTF_8));
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        long startCurrentTimeMillis = dateFormat.parse(module.getStartTime()).getTime();
                        long endCurrentTimeMillis = dateFormat.parse(module.getStartTime()).getTime();
                        long millis = endCurrentTimeMillis - startCurrentTimeMillis;
                        if (millis > 60_000 * 20) {
                            String msg = "the time span within 20 minutes";
                            exchange.sendResponseHeaders(203, msg.length());
                            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
                            return;
                        }
                        List<KeyValueModule> keyValueModules = new ArrayList<>();
                        ExecutorService threadPool = Executors.newFixedThreadPool(10);
                        for (long ms = startCurrentTimeMillis; ms < endCurrentTimeMillis; ms++) {
                            long finalMs = ms;
                            threadPool.submit(() -> {
                                        String key = module.getPrefix() + finalMs;
                                        byte[] values = redisEngine.get(module.getPrefix() + key);
                                        if (values.length != 0) {
                                            keyValueModules.add(new KeyValueModule(key,
                                                    new String(bytes, StandardCharsets.UTF_8)));
                                        }
                                    }
                            );
                        }
                        threadPool.shutdown();
                        String json = JacksonService.toJson(keyValueModules);
                        exchange.sendResponseHeaders(200, json.length());
                        exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
                    } catch (ParseException e) {
                        exchange.sendResponseHeaders(400, e.getMessage().length());
                        exchange.getResponseBody().write(e.getMessage().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            case "PUT" -> {
                InputStream inputStream = exchange.getRequestBody();
                byte[] bytes = inputStream.readAllBytes();
                KeyValueModule keyValueModule = JacksonService.toPojo(bytes, KeyValueModule.class);
                if (keyValueModule == null) {
                    exchange.sendResponseHeaders(400, 0);
                    return;
                }
                redisEngine.set(keyValueModule.getKey(), keyValueModule.getValue());
            }
            default -> exchange.sendResponseHeaders(405, 0);
        }
        exchange.close();
    }
}
