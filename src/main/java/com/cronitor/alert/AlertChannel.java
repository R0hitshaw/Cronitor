package com.cronitor.alert;

/**
 * Strategy interface for notification channels.
 *
 * Each implementation handles one channel type (EMAIL, SLACK, SNS, WEBHOOK).
 * AlertDispatcher discovers all implementations via Spring's DI and routes
 * alerts to the correct one based on the job's configured channels.
 *
 * To add a new channel: implement this interface, annotate with @Component.
 * No other changes needed anywhere.
 */
public interface AlertChannel {

    /**
     * The channel type this implementation handles.
     * Must match the channel_type values stored in notification_channels table.
     * Example: "EMAIL", "SLACK", "SNS"
     */
    String supports();

    /**
     * Deliver the alert. Implementations are responsible for:
     * - Extracting their config from the event's job context
     * - Handling their own errors gracefully (log, don't rethrow)
     */
    void send(AlertEvent event, String configJson);
}
