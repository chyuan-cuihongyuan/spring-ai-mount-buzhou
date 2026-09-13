package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP 连接能力协商快照（spec 822 / T1145，LSP initialize capabilities 思想
 * ——建连时记录「这个 server 声明了什么」）：单快照面——工具数/hint 覆盖数/
 * readOnly/destructive 计数 + 排序名册 + 稳定指纹。与 706 McpDirectoryDiff
 * 正交：706 比<b>两份</b>快照出 diff；本类产出<b>一份</b>可存的快照+指纹
 * （作为 706 的输入基线形状）。
 *
 * <p>纯函数（{@code of(connection, atMs)}）：只读连接 seam 的三个观察点
 * （toolCallbacks/listToolNames/toolHints），零连接侵入；指纹=排序名册
 * join（确定性，无加密语义）。
 */
public final class McpCapabilitySnapshot {

    /** 不可变快照。 */
    public record Snapshot(String server, long atMillis, int toolCount, int hintCount,
                           int readOnlyCount, int destructiveCount, List<String> toolNames,
                           String fingerprint) {
    }

    private McpCapabilitySnapshot() {
    }

    /** 采集快照（server 名/连接/atMs；连接观察点异常按空值降级——不炸采集）。 */
    public static Snapshot of(String server, McpConnection connection, long atMillis) {
        Objects.requireNonNull(connection, "connection");
        List<String> names;
        try {
            names = new ArrayList<>(connection.listToolNames());
        } catch (RuntimeException e) {
            names = new ArrayList<>();
        }
        names.sort(String::compareTo);
        if (names.isEmpty()) {
            try {
                for (ToolCallback cb : connection.toolCallbacks()) {
                    if (cb != null && cb.getToolDefinition() != null) {
                        names.add(cb.getToolDefinition().name());
                    }
                }
                names.sort(String::compareTo);
            } catch (RuntimeException e) {
                // 双路均不可用——空名册（诚实口径）
            }
        }
        Map<String, McpToolHints> hints;
        try {
            hints = connection.toolHints();
        } catch (RuntimeException e) {
            hints = Map.of();
        }
        int readOnly = 0;
        int destructive = 0;
        for (McpToolHints hint : hints.values()) {
            if (hint == null) {
                continue;
            }
            if (hint.readOnlyHint()) {
                readOnly++;
            }
            if (hint.destructiveHint()) {
                destructive++;
            }
        }
        return new Snapshot(server == null ? "" : server, atMillis, names.size(),
                hints.size(), readOnly, destructive, List.copyOf(names), fingerprint(names));
    }

    /** 稳定指纹（排序名册 join「,」——确定性；空名册 = 空串）。 */
    public static String fingerprint(List<String> sortedNames) {
        List<String> sorted = new ArrayList<>(sortedNames);
        sorted.sort(String::compareTo);
        return String.join(",", sorted);
    }
}
