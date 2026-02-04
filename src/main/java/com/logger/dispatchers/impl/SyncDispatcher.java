package com.logger.dispatchers.impl;

import java.util.Set;

import com.logger.appenders.Appender;
import com.logger.dispatchers.Dispatcher;
import com.logger.models.LogRecord;

public class SyncDispatcher implements Dispatcher {

    @Override
    public void dispatch(LogRecord logRecord, Set<Appender> appenders) {
        appenders.forEach(appender -> {
            try {
                appender.append(logRecord);
            } catch (Exception e) {
                System.err.println("Failed to append log record: " + e.getMessage());
            }
        });
    }

}
