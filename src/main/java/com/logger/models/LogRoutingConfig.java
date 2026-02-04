package com.logger.models;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.logger.appenders.Appender;
import com.logger.enums.LogLevel;

public class LogRoutingConfig {
    private final ConcurrentHashMap<LogLevel, Set<Appender>> routingMap = new ConcurrentHashMap<>();

    public void addRoute(final LogLevel level, final Appender appender) {
        routingMap.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(appender);
    }

    public Set<Appender> getRoute(final LogLevel level) {
        return routingMap.getOrDefault(level, ConcurrentHashMap.newKeySet());
    }

}
