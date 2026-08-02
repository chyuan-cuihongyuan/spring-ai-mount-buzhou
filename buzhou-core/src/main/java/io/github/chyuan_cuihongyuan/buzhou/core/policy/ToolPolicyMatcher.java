package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;

public final class ToolPolicyMatcher {

    private static final Logger LOG = System.getLogger(ToolPolicyMatcher.class.getName());

    private ToolPolicyMatcher() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> match(Map<String, Object> toolPolicies, String toolName) {
        Object exact = toolPolicies.get(toolName);
        Map<String, Object> exactMap = asPolicyMap(exact, toolName);
        if (exactMap != null) {
            return exactMap;
        }
        String bestPattern = null;
        int bestPrefixLength = -1;
        for (Map.Entry<String, Object> entry : toolPolicies.entrySet()) {
            String pattern = entry.getKey();
            if (!pattern.contains("*") || asPolicyMap(entry.getValue(), pattern) == null) {
                continue;
            }
            if (!globMatches(pattern, toolName)) {
                continue;
            }
            int prefixLength = pattern.indexOf('*');
            if (prefixLength > bestPrefixLength) {
                bestPrefixLength = prefixLength;
                bestPattern = pattern;
            }
        }
        if (bestPattern == null) {
            return Map.of();
        }
        return asPolicyMap(toolPolicies.get(bestPattern), bestPattern);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asPolicyMap(Object value, String key) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            LOG.log(Level.WARNING, "Invalid tool policy entry ignored: " + key);
            return null;
        }
        return (Map<String, Object>) value;
    }

    static boolean globMatches(String pattern, String name) {
        String[] parts = pattern.split("\\*", -1);
        int index = 0;
        if (!parts[0].isEmpty()) {
            if (!name.startsWith(parts[0])) {
                return false;
            }
            index = parts[0].length();
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            int found = name.indexOf(part, index);
            if (found < 0) {
                return false;
            }
            index = found + part.length();
        }
        if (!parts[parts.length - 1].isEmpty()) {
            return name.endsWith(parts[parts.length - 1]);
        }
        return true;
    }
}
