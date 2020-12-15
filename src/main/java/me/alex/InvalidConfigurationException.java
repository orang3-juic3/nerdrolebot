package me.alex;

import java.io.IOException;

public class InvalidConfigurationException extends IOException {
    public InvalidConfigurationException(String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
    }
    public InvalidConfigurationException(String errorMessage) {
        super(errorMessage);
    }
    public InvalidConfigurationException() {
        super("");
    }
}
