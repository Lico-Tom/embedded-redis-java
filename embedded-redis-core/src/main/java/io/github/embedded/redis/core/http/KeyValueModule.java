package io.github.embedded.redis.core.http;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KeyValueModule {

    private String key;

    private String value;

    public KeyValueModule() {
    }

    public KeyValueModule(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
