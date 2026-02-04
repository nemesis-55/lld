package com.logger.appenders.impl;

import com.logger.appenders.Appender;
import com.logger.models.LogRecord;

public class ConsoleAppender implements Appender {

    @Override
    public void append(LogRecord logRecord) {
        String logString = String.format("[%d] [%s] [%s]: %s",
                logRecord.getTimestamp(),
                logRecord.getLevel().name(),
                logRecord.getLoggerName(),
                logRecord.getMessage());
        System.out.println(logString);
        if (logRecord.getThrowable() != null) {
            logRecord.getThrowable().printStackTrace(System.out);
        }
    }

}
