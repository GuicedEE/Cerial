package com.guicedee.cerial;

/**
 * Exception thrown when a serial port operation fails.
 */
public class SerialPortException
		extends RuntimeException{
    /**
     * Creates a new serial port exception.
     */
    public SerialPortException() {
    }

    /**
     * Creates a new serial port exception with a message.
     *
     * @param message the detail message
     */
    public SerialPortException(String message) {
        super(message);
    }

    /**
     * Creates a new serial port exception with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SerialPortException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new serial port exception with a cause.
     *
     * @param cause the underlying cause
     */
    public SerialPortException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new serial port exception with full control over suppression and stack trace.
     *
     * @param message            the detail message
     * @param cause              the underlying cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace is writable
     */
    public SerialPortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
