package com.piti.java.librarymanagement.exception;

@SuppressWarnings("serial")
public class OperationNotPermittedException extends RuntimeException {

    public OperationNotPermittedException() {
    	
    }

    public OperationNotPermittedException(String message) {
        super(message);
    }
}
