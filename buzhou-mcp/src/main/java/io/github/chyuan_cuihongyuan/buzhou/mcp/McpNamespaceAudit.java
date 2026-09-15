package io.github.chyuan_cuihongyuan.buzhou.mcp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MCP 命名空间冲突普查（L 会话 1700 系 R38 = effort #1737 / spec 1737 /
 * 票 T2675 + T2676 / impl 1337）——npm scope 冲突思想：多 MCP 服务端
 * 暴露同名工具时（两个 server 都有 search），工具名路由有歧义——
 * 「哪些工具名被多个服务端占用」是装配冲突的第一信号。
 * {@link McpDirectoryDiff} 管目录差分，本面管同名冲突。
 *
 * <p>实例面线程安全：`register(server, tool)` 逐工具登记（缺名归
 * _anonymous_ 服务端）+`census()` 吐工具总数/冲突工具数（多服务端占用）/
 * 单工具最多服务端数。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class McpNamespaceAudit {

    private final Object lock = new Object();
    private final Map<String, Set<String>> toolToServers = new HashMap<>();

    /** 登记一个服务端工具（server 缺名归 _anonymous_）。 */
    public void register(String server, String tool) {
        String serverName = server == null || server.isBlank() ? "_anonymous_" : server;
        String toolName = tool == null || tool.isBlank() ? "_anonymous_tool_" : tool;
        synchronized (lock) {
            toolToServers.computeIfAbsent(toolName, k -> new HashSet<>()).add(serverName);
        }
    }

    /**
     * @param tools             登记工具名总数
     * @param collidingTools    被多个服务端占用的工具名数（冲突）
     * @param maxServersPerTool 单工具最多服务端数
     */
    public record NamespaceCensus(int tools, int collidingTools, int maxServersPerTool) {
    }

    /** 快照。 */
    public NamespaceCensus census() {
        synchronized (lock) {
            int colliding = 0;
            int max = 0;
            for (Set<String> servers : toolToServers.values()) {
                if (servers.size() > 1) {
                    colliding++;
                }
                max = Math.max(max, servers.size());
            }
            return new NamespaceCensus(toolToServers.size(), colliding, max);
        }
    }

    /** 指定工具名的占用服务端集合（只读；未登记返回空）。 */
    public Set<String> serversOf(String tool) {
        synchronized (lock) {
            Set<String> servers = toolToServers.get(tool);
            return servers == null ? Set.of() : Set.copyOf(servers);
        }
    }
}
