package com.ruptech.tttalk_android.exception;

public class XMPPException extends Exception {
    private static final long serialVersionUID = 1L;

    public XMPPException(String message) {
        super(message);
    }

    public XMPPException(String message, Throwable cause) {
        super(message, cause);
    }
}
