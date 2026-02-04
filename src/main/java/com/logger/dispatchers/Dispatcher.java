package com.logger.dispatchers;

import java.util.Set;

import com.logger.appenders.Appender;
import com.logger.models.LogRecord;


public interface Dispatcher {
    void dispatch(LogRecord logRecord, Set<Appender> appenders);
}
