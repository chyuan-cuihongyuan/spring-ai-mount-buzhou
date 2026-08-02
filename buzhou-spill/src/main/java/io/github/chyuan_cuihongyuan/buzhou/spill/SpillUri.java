package io.github.chyuan_cuihongyuan.buzhou.spill;

public record SpillUri(String agentName, String sessionId, String toolCallId) {

    private static final String SCHEME = "spill://";
    private static final String ALLOWED = "[A-Za-z0-9._-]+";

    public SpillUri {
        validate("agentName", agentName);
        validate("sessionId", sessionId);
        validate("toolCallId", toolCallId);
    }

    private static void validate(String field, String value) {
        if (value == null || !value.matches(ALLOWED) || value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException(
                    "Illegal " + field + " (allowed charset [A-Za-z0-9._-]): " + value);
        }
    }

    public static SpillUri parse(String uri) {
        if (uri == null || !uri.startsWith(SCHEME)) {
            throw new IllegalArgumentException("Not a spill URI: " + uri);
        }
        String[] parts = uri.substring(SCHEME.length()).split("/");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Spill URI must have 3 segments: " + uri);
        }
        return new SpillUri(parts[0], parts[1], parts[2]);
    }

    @Override
    public String toString() {
        return SCHEME + agentName + "/" + sessionId + "/" + toolCallId;
    }
}
