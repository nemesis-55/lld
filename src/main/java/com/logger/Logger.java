package com.logger;


import com.logger.enums.LogLevel;
import com.logger.models.LogRecord;

public class Logger {
    private static volatile Logger INSTANCE = null;
    private final  String name;
    private final LogManager logManager;

    private Logger(String name) {
        this.name = name;
        this.logManager = LogManager.getInstance();
    }

    public static Logger getInstance(String name) {
        if (INSTANCE == null) {
            synchronized (Logger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Logger(name);
                }
            }
        }
        return INSTANCE;
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public void debug(String msg) {
        log(LogLevel.INFO, msg);
    }

    public void error(String msg, Throwable t) {
        log(LogLevel.ERROR, msg, t);
    }

    private void log(LogLevel level, String msg) {
        LogRecord record = LogRecord.create(msg, level, name);
        logManager.log(record);
    }

     private void log(LogLevel level, String msg, Throwable t) {
        LogRecord record = LogRecord.create(msg, level, name, t);
        logManager.log(record);
    }
}
