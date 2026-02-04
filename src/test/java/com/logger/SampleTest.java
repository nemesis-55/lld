package com.logger;

import org.junit.jupiter.api.Test;

import com.logger.appenders.impl.ConsoleAppender;
import com.logger.dispatchers.impl.SyncDispatcher;
import com.logger.enums.LogLevel;
import com.logger.models.LogRoutingConfig;

public class SampleTest {

    @Test
    void sampleTest() {
        LogRoutingConfig config = new LogRoutingConfig();
        config.addRoute(LogLevel.INFO, new ConsoleAppender());
        config.addRoute(LogLevel.DEBUG, new ConsoleAppender());
        config.addRoute(LogLevel.ERROR, new ConsoleAppender());

        LogManager.initialize(config, new SyncDispatcher());
        Logger logger = Logger.getInstance(this.getClass().getName());

        logger.info("This is an info message.");
        logger.debug("This is a debug message.");
        logger.error("This is an error message.", new Exception("Sample exception"));
    }

}
