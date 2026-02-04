package com.logger.models;

import com.logger.enums.LogLevel;

import lombok.Getter;

@Getter
public class LogRecord {
    private final String message;
    private final LogLevel level; 
    private final long timestamp;
    private final String loggerName;
    private final Throwable throwable;

    private LogRecord(final String message, final LogLevel level, final long timestamp,
            final String loggerName, final Throwable throwable) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
        this.loggerName = loggerName;
        this.throwable = throwable;
    }

    public static LogRecord create(final String message, final LogLevel level,
            final String loggerName, final Throwable throwable) {
        return new LogRecord(message, level, System.currentTimeMillis(), loggerName, throwable);
    }
    
    public static LogRecord create(final String message, final LogLevel level,
            final String loggerName) {
        return new LogRecord(message, level, System.currentTimeMillis(), loggerName, null);
    }

}
