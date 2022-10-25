package io.github.embedded.redis.core.http;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TimeIntervalModule {
    /**
     * example: 2022-10-24 19:45:20
     */
    private String startTime;
    /**
     * example: 2022-10-24 19:55:20
     */
    private String endTime;

    /**
     * example: client:
     */
    private String prefix;
}
