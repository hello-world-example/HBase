package xyz.kail.demo.hbase.tools;

import org.slf4j.impl.SimpleLogger;
import org.slf4j.impl.SimpleLoggerConfiguration;

public class Slf4jSimpleTool {

    static {
        init();
    }

    /**
     * @see SimpleLoggerConfiguration
     */
    public static void init() {

        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss.SSS");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
    }
}
