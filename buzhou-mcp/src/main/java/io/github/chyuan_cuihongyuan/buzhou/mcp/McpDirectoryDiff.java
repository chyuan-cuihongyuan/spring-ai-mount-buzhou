package io.github.chyuan_cuihongyuan.buzhou.mcp;

import java.util.ArrayList;
import java.util.Comparator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * MCP 工具目录差异报告（spec 706 / T1012，ArgoCD diff/Terraform plan 思想）：
 * 两份 toolHints() 快照的结构化差异——per-server 同步态 + 逐工具变更 +
 * 危险方向翻转标记（readOnly true→false / destructive false→true）。
 * 纯函数无状态：baseline 由调用方供给（上次快照）——周期快照编排归宿主。
 *
 * <p>对比口径与 600 同边界：title + 三 hint（工具描述/入参 schema 不在自报
 * 快照内）。排序确定性：server/工具名字典序——快照断言可复现。
 */
public final class McpDirectoryDiff {

    /** server 同步态。 */
    public enum SyncStatus { IN_SYNC, DRIFTED, SERVER_NEW, SERVER_GONE }

    /** 变更类别。 */
    public enum ChangeKind { ADDED, REMOVED, HINT_CHANGED }

    /**
     * 单工具变更（risky=危险方向翻转：readOnly true→false 或 destructive false→true
     * ——「工具静默变危险」的供应链审计焦点）。
     */
    public record ToolChange(String server, String tool, ChangeKind kind, String detail, boolean risky) {
    }

    /** 单 server 差异（changes 字典序）。 */
    public record ServerDiff(String server, SyncStatus status, List<ToolChange> changes) {
    }

    /** 不可变报告（servers 字典序；聚合计数）。 */
    public record Report(List<ServerDiff> servers, int added, int removed, int hintChanged, int risky) {
    }

    private McpDirectoryDiff() {
    }

    /** 差异计算（null fail-fast；空 vs 空 = 空 Report）。 */
    public static Report diff(Map<String, Map<String, McpToolHints>> baseline,
                              Map<String, Map<String, McpToolHints>> current) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(current, "current");
        Map<String, Map<String, McpToolHints>> base = new TreeMap<>(baseline);
        Map<String, Map<String, McpToolHints>> cur = new TreeMap<>(current);

        List<ServerDiff> servers = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int hintChanged = 0;
        int risky = 0;

        Set<String> allServers = new java.util.TreeSet<>();
        allServers.addAll(base.keySet());
        allServers.addAll(cur.keySet());
        for (String server : allServers) {
            Map<String, McpToolHints> baseTools = base.getOrDefault(server, Map.of());
            Map<String, McpToolHints> curTools = cur.getOrDefault(server, Map.of());
            if (curTools.isEmpty() && !base.containsKey(server)) {
                continue; // 双侧皆无（理论不可达）——防御跳过
            }
            if (!base.containsKey(server)) {
                servers.add(new ServerDiff(server, SyncStatus.SERVER_NEW, List.of()));
                continue;
            }
            if (curTools.isEmpty() && !cur.containsKey(server)) {
                servers.add(new ServerDiff(server, SyncStatus.SERVER_GONE, List.of()));
                continue;
            }
            List<ToolChange> changes = new ArrayList<>();
            Set<String> tools = new java.util.TreeSet<>();
            tools.addAll(baseTools.keySet());
            tools.addAll(curTools.keySet());
            for (String tool : tools) {
                McpToolHints before = baseTools.get(tool);
                McpToolHints after = curTools.get(tool);
                if (before == null) {
                    changes.add(new ToolChange(server, tool, ChangeKind.ADDED,
                            describe(after), isRiskyAddition(after)));
                    added++;
                    if (isRiskyAddition(after)) {
                        risky++;
                    }
                } else if (after == null) {
                    changes.add(new ToolChange(server, tool, ChangeKind.REMOVED,
                            describe(before), false));
                    removed++;
                } else if (!before.equals(after)) {
                    String detail = changedFields(before, after);
                    boolean flip = isRiskyFlip(before, after);
                    changes.add(new ToolChange(server, tool, ChangeKind.HINT_CHANGED, detail, flip));
                    hintChanged++;
                    if (flip) {
                        risky++;
                    }
                }
            }
            SyncStatus status = changes.isEmpty() ? SyncStatus.IN_SYNC : SyncStatus.DRIFTED;
            servers.add(new ServerDiff(server, status, List.copyOf(changes)));
        }
        return new Report(List.copyOf(servers), added, removed, hintChanged, risky);
    }

    private static boolean isRiskyAddition(McpToolHints hints) {
        return hints != null && (!hints.readOnlyHint() || hints.destructiveHint());
    }

    private static boolean isRiskyFlip(McpToolHints before, McpToolHints after) {
        return (before.readOnlyHint() && !after.readOnlyHint())
                || (!before.destructiveHint() && after.destructiveHint());
    }

    /** 逐字段变化明细（只列变化字段；Comparator 兼容 null——防御）。 */
    private static String changedFields(McpToolHints before, McpToolHints after) {
        List<String> parts = new ArrayList<>();
        if (!Objects.equals(before.title(), after.title())) {
            parts.add("title: " + before.title() + "→" + after.title());
        }
        if (before.readOnlyHint() != after.readOnlyHint()) {
            parts.add("readOnlyHint: " + before.readOnlyHint() + "→" + after.readOnlyHint());
        }
        if (before.destructiveHint() != after.destructiveHint()) {
            parts.add("destructiveHint: " + before.destructiveHint() + "→" + after.destructiveHint());
        }
        if (before.idempotentHint() != after.idempotentHint()) {
            parts.add("idempotentHint: " + before.idempotentHint() + "→" + after.idempotentHint());
        }
        if (before.openWorldHint() != after.openWorldHint()) {
            parts.add("openWorldHint: " + before.openWorldHint() + "→" + after.openWorldHint());
        }
        return String.join(", ", parts);
    }

    /** 快照描述（ADDED/REMOVED 的 detail）。 */
    private static String describe(McpToolHints hints) {
        if (hints == null) {
            return "";
        }
        return "title=" + hints.title() + ", readOnly=" + hints.readOnlyHint()
                + ", destructive=" + hints.destructiveHint() + ", idempotent=" + hints.idempotentHint();
    }
}
