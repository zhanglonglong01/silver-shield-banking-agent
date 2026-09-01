package com.fintechathon.silvershield.tool.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ToolId(String name, String version) implements Comparable<ToolId> {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{2,63}");
    private static final Pattern VERSION = Pattern.compile("[1-9][0-9]*\\.[0-9]+");

    public ToolId {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        if (!NAME.matcher(name).matches() || !VERSION.matcher(version).matches()) {
            throw new IllegalArgumentException("Malformed tool identifier");
        }
    }

    @Override
    public int compareTo(ToolId other) {
        int byName = name.compareTo(other.name);
        return byName != 0 ? byName : version.compareTo(other.version);
    }

    @Override
    public String toString() {
        return name + "@" + version;
    }
}
