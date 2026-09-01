package com.fintechathon.silvershield.common.api;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super("Resource is not available");
    }
}
