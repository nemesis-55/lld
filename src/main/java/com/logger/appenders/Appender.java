package com.logger.appenders;

import com.logger.models.LogRecord;

public interface Appender {

    void append(LogRecord logRecord);

}
