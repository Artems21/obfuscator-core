package org.jxfuscator.logger;

public class Logger {
    private String prefix;
    public Logger(String name) {
        this.prefix = "[" +  name + "] ";
    }

    public void info(String msg) {
        System.out.println(prefix + msg);
    }
}
