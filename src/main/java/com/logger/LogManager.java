package com.logger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


import com.logger.appenders.Appender;
import com.logger.dispatchers.Dispatcher;
import com.logger.models.LogRecord;
import com.logger.models.LogRoutingConfig;


public class LogManager {
    private volatile static LogManager INSTANCE = null;
    private final AtomicReference<LogRoutingConfig> routingRef;
    private final Dispatcher dispatcher;

    private LogManager(LogRoutingConfig initialConfig, Dispatcher dispatcher) {
        this.routingRef = new AtomicReference<>(initialConfig);
        this.dispatcher = dispatcher;
    }

    public static void initialize(LogRoutingConfig initialConfig, Dispatcher dispatcher) {
        if (INSTANCE == null) {
            synchronized (LogManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LogManager(initialConfig, dispatcher);
                }
            }
        }
    }

    public static LogManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("LogManager is not initialized. Call the constructor first.");
        }
        return INSTANCE;
    }

    void log(final LogRecord record) {
        LogRoutingConfig snapshot = routingRef.get();
        Set<Appender> appenders = snapshot.getRoute(record.getLevel());
        dispatcher.dispatch(record, appenders);
    }

    public AtomicReference<LogRoutingConfig> getRoutingRef() {
        return routingRef;
    }

    public void updateRoutingConfig(final LogRoutingConfig newConfig) {
        routingRef.set(newConfig);
    }


}
