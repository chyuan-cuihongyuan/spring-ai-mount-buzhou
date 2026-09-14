package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具目录重名审计（spec 1428 / T2157 / impl 1081）——Spring 容器 bean
 * 重名 fail-fast / Maven Enforcer duplicate 检查思想：{@code HarnessToolCallingManager}
 * 以 HashMap 按 toolDefinition.name() 建索引——**重名工具静默互相覆盖**
 * （后注册者胜，先注册者被遮蔽且无任何信号）。本地工具与多台 MCP server
 * 工具天然可能同名（read_file 两边都有是常态），遮蔽导致「调的到底是
 * 哪个」不可解释。本审计把重名组显形，供装配期巡查。
 *
 * <p>纯函数零状态：吃工具名清单（调用方从 callbacks/registry 抽取）；
 * 只列重名组（≥2 同名），修复 = 重命名或显式排除（审计不裁决）。
 */
public final class ToolCatalogDuplicateAudit {

    private ToolCatalogDuplicateAudit() {
    }

    /**
     * @param toolName 被重复注册的工具名
     * @param count    注册次数
     */
    public record DuplicateGroup(String toolName, int count) {
    }

    /**
     * @param totalTools     工具名总数（含重复）
     * @param distinctTools  去重后工具名数
     * @param duplicates     重名组（名字典序）
     * @param duplicateCount 重名组数（0 = 目录健康）
     */
    public record Report(int totalTools, int distinctTools,
                         List<DuplicateGroup> duplicates, int duplicateCount) {
    }

    /** 审计入口：工具名清单（顺序无关）。 */
    public static Report analyze(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return new Report(0, 0, List.of(), 0);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String name : toolNames) {
            counts.merge(name, 1, Integer::sum);
        }
        List<DuplicateGroup> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .map(e -> new DuplicateGroup(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DuplicateGroup::toolName))
                .toList();
        return new Report(toolNames.size(), counts.size(),
                List.copyOf(duplicates), duplicates.size());
    }
}
