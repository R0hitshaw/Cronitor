package com.cronitor.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class CronExpressionUtil {

    private CronExpressionUtil() {}

    /**
     * Computes the next scheduled instant after {@code from} for the given
     * standard 6-field Spring cron expression (seconds included).
     *
     * Spring cron: "0 0 2 * * *"  → every day at 02:00:00 UTC
     *
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static Instant nextAfter(String cronExpression, Instant from) {
        CronExpression expr = CronExpression.parse(cronExpression);
        ZonedDateTime next = expr.next(ZonedDateTime.ofInstant(from, ZoneOffset.UTC));
        if (next == null) {
            throw new IllegalArgumentException(
                    "Cron expression '" + cronExpression + "' has no future occurrences");
        }
        return next.toInstant();
    }
}
