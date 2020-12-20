package me.alex;

import java.io.IOException;

/**
 * An exception class that is a subclass of IOException. It is primarily thrown when the configuration file is written wrong.
 * @see IOException
 */
public class InvalidConfigurationException extends IOException {
    /**
     * Generates an InvalidConfigurationException with an error message and a throwable.
     * @see Throwable
     * @param errorMessage The error message.
     * @param throwable The throwable.
     */
    public InvalidConfigurationException(String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
    }
    /**
     * Generates an InvalidConfigurationException with only an error message.
     * @param errorMessage The error message.
     */
    public InvalidConfigurationException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Generates an empty InvalidConfigurationException.
     */
    public InvalidConfigurationException() {
        super("");
    }
}
