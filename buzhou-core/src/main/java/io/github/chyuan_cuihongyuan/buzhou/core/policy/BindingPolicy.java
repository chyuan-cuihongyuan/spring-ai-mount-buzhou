package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;
import java.util.Map;

public record BindingPolicy(
        String appId,
        String agentName,
        Map<String, Object> mechanismOverrides,
        List<String> skillNames,
        List<McpServerBinding> mcpServers,
        long version) {

    public BindingPolicy {
        mechanismOverrides = mechanismOverrides == null ? Map.of() : Map.copyOf(mechanismOverrides);
        skillNames = skillNames == null ? List.of() : List.copyOf(skillNames);
        mcpServers = mcpServers == null ? List.of() : List.copyOf(mcpServers);
    }

    public static BindingPolicy empty(String appId, String agentName) {
        return new BindingPolicy(appId, agentName, Map.of(), List.of(), List.of(), 0);
    }

    public static String key(String appId, String agentName) {
        return appId + ":" + agentName;
    }
}
