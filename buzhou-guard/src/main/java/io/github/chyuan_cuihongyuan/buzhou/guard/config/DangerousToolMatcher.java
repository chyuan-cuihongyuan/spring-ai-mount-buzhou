package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.util.List;
import java.util.Optional;

/**
 * 危险工具匹配器：工具名 → 命中的清单条目。
 *
 * <p>匹配优先级：精确名 > 最长前缀通配 > 不命中。通配语法 = 单星号 glob（对齐
 * {@code ToolPolicyMatcher.globMatches} 的私有实现：split on {@code *}，首段须前缀、
 * 末段须后缀、中段 indexOf 顺序匹配；最长前缀优先消歧）。
 */
public final class DangerousToolMatcher {

    private final List<DangerousToolEntry> entries;

    public DangerousToolMatcher(List<DangerousToolEntry> entries) {
        this.entries = entries == null ? List.of() : entries;
    }

    /** 匹配工具名；无命中返回 {@link Optional#empty()}。 */
    public Optional<DangerousToolEntry> match(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        // 1. 精确名优先
        for (DangerousToolEntry entry : entries) {
            if (toolName.equals(entry.name())) {
                return Optional.of(entry);
            }
        }
        // 2. 通配：最长前缀优先
        DangerousToolEntry best = null;
        int bestPrefixLen = -1;
        for (DangerousToolEntry entry : entries) {
            String pattern = entry.name();
            if (pattern.indexOf('*') < 0) {
                continue; // 非通配已在精确匹配处理
            }
            if (globMatches(pattern, toolName)) {
                int prefixLen = pattern.indexOf('*');
                if (prefixLen > bestPrefixLen) {
                    bestPrefixLen = prefixLen;
                    best = entry;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 单星号 glob（对齐 ToolPolicyMatcher.globMatches 私有实现）。 */
    static boolean globMatches(String pattern, String name) {
        String[] parts = pattern.split("\\*", -1);
        if (parts.length == 0) {
            return true;
        }
        // 首段须为 name 前缀
        if (!parts[0].isEmpty() && !name.startsWith(parts[0])) {
            return false;
        }
        int cursor = parts[0].length();
        for (int i = 1; i < parts.length; i++) {
            String segment = parts[i];
            if (segment.isEmpty()) {
                continue; // 连续星号折叠
            }
            int found = name.indexOf(segment, cursor);
            if (found < 0) {
                return false;
            }
            cursor = found + segment.length();
        }
        // 末段非空（模式不以 * 结尾）时须为后缀
        String last = parts[parts.length - 1];
        if (!last.isEmpty() && !name.endsWith(last)) {
            return false;
        }
        return true;
    }
}
