package org.softstar.stp.exception;

public class CorruptedPacketException extends RuntimeException {
    public CorruptedPacketException() {
    }

    public CorruptedPacketException(String message) {
        super(message);
    }
}
