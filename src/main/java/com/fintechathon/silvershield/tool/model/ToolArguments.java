package com.fintechathon.silvershield.tool.model;

public interface ToolArguments {

    default void validateSemantics() {
        // Most argument records only require Jakarta Bean Validation.
    }
}
