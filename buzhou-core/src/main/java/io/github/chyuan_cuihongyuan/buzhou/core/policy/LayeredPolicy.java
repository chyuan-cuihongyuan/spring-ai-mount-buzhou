package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LayeredPolicy(
        Map<String, Object> defaults,
        Map<String, Object> yml,
        Map<String, Object> binding) {

    public LayeredPolicy {
        defaults = defaults == null ? Map.of() : defaults;
        yml = yml == null ? Map.of() : yml;
        binding = binding == null ? Map.of() : binding;
    }

    public Object get(String dottedKey) {
        Object value = lookup(binding, dottedKey);
        if (value != null) {
            return value;
        }
        value = lookup(yml, dottedKey);
        if (value != null) {
            return value;
        }
        return lookup(defaults, dottedKey);
    }

    public Map<String, Object> getMap(String dottedKey) {
        Map<String, Object> merged = new LinkedHashMap<>();
        deepMerge(merged, lookupMap(defaults, dottedKey));
        deepMerge(merged, lookupMap(yml, dottedKey));
        deepMerge(merged, lookupMap(binding, dottedKey));
        return merged;
    }

    private Object lookup(Map<String, Object> layer, String dottedKey) {
        Object current = layer;
        for (String segment : dottedKey.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lookupMap(Map<String, Object> layer, String dottedKey) {
        Object value = lookup(layer, dottedKey);
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            if (value instanceof Map && target.get(key) instanceof Map) {
                deepMerge((Map<String, Object>) target.get(key), (Map<String, Object>) value);
            } else {
                target.put(key, value);
            }
        });
    }
}
