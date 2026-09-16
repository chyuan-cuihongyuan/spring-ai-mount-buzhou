package io.github.chyuan_cuihongyuan.buzhou.mcp.provenance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 工具溯源索引（spec 2034 / T3169 / impl 1585）——MCP 多 server 工具
 * 记账思想：tool → 提供 server 双向账——server 摘除时其独供工具即
 * 孤儿（调用方无从路由）即刻显形；多 server 同名工具冲突面（解析歧
 * 义源）常驻读数。目录差异报告（706）看两次快照间变化，本件看任一
 * 时刻的静态归属与摘除后果。
 *
 * <p>synchronized 小临界区；注册序稳定（快照字典序）。
 */
public final class ToolProvenanceIndex {

    private final Map<String, Set<String>> toolToServers = new HashMap<>();
    private final Map<String, Set<String>> serverToTools = new LinkedHashMap<>();

    /** 注册 server 的工具清单（重复注册覆盖——重连刷新口径）。契约：serverId/toolNames 非空。 */
    public synchronized void register(String serverId, List<String> toolNames) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("serverId 不能为空");
        }
        if (toolNames == null) {
            throw new IllegalArgumentException("toolNames 不能为 null");
        }
        unregister(serverId); // 覆盖口径：先撤旧账再铺新
        Set<String> tools = new HashSet<>(toolNames);
        tools.forEach(t -> {
            if (t == null || t.isBlank()) {
                throw new IllegalArgumentException("工具名不能为空");
            }
        });
        serverToTools.put(serverId, tools);
        tools.forEach(tool -> toolToServers.computeIfAbsent(tool, k -> new HashSet<>()).add(serverId));
    }

    /**
     * 摘除 server：返回其**独供**工具（孤儿——无其他 provider，调用方
     * 无从路由）；共供工具不孤儿（其余 provider 仍在）。
     */
    public synchronized Set<String> unregister(String serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("serverId 不能为 null");
        }
        Set<String> tools = serverToTools.remove(serverId);
        if (tools == null) {
            return Set.of();
        }
        Set<String> orphans = new TreeSet<>();
        for (String tool : tools) {
            Set<String> providers = toolToServers.get(tool);
            providers.remove(serverId);
            if (providers.isEmpty()) {
                toolToServers.remove(tool);
                orphans.add(tool); // 独供工具失源
            }
        }
        return orphans;
    }

    /** 工具的提供者集合（空 = 无源——孤儿/未注册）。 */
    public synchronized Set<String> providersOf(String toolName) {
        if (toolName == null) {
            throw new IllegalArgumentException("toolName 不能为 null");
        }
        return Set.copyOf(toolToServers.getOrDefault(toolName, Set.of()));
    }

    /** 多源同名工具面（解析歧义源——>1 provider 的工具字典序快照）。 */
    public synchronized Map<String, Integer> conflictingTools() {
        Map<String, Integer> conflicts = new LinkedHashMap<>();
        new TreeSet<>(toolToServers.keySet()).forEach(tool -> {
            int providers = toolToServers.get(tool).size();
            if (providers > 1) {
                conflicts.put(tool, providers);
            }
        });
        return conflicts;
    }

    /** server 数（注册面）。 */
    public synchronized int serverCount() {
        return serverToTools.size();
    }

    /** 工具总数（去重跨 server）。 */
    public synchronized int toolCount() {
        return toolToServers.size();
    }
}
