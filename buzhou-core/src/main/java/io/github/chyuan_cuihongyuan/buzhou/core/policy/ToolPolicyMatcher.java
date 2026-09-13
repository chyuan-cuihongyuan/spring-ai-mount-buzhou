package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class ToolPolicyMatcher {

    private static final Logger LOG = System.getLogger(ToolPolicyMatcher.class.getName());

    /** 最近决策环容量（有界纪律：决策读面不无界增长）。 */
    static final int RECENT_CAPACITY = 32;

    private static final Object STATS_LOCK = new Object();
    private static long exactHits;
    private static long globHits;
    private static long noneHits;
    private static final Deque<ToolPolicyMatchDecision> RECENT = new ArrayDeque<>();

    private ToolPolicyMatcher() {
    }

    /**
     * 匹配工具级策略（精确名优先，通配按最长前缀胜出，未命中返回空 Map）。
     * 返回值语义不变；判定单点同步累加进程级决策读面（{@link #stats()}——
     * OPA decision log 借鉴，零配置零行为变化）。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> match(Map<String, Object> toolPolicies, String toolName) {
        Object exact = toolPolicies.get(toolName);
        Map<String, Object> exactMap = asPolicyMap(exact, toolName);
        if (exactMap != null) {
            record(new ToolPolicyMatchDecision(toolName, ToolPolicyMatchDecision.Outcome.EXACT, toolName));
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
            record(new ToolPolicyMatchDecision(toolName, ToolPolicyMatchDecision.Outcome.NONE, ""));
            return Map.of();
        }
        record(new ToolPolicyMatchDecision(toolName, ToolPolicyMatchDecision.Outcome.GLOB, bestPattern));
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

    /** 决策读面快照（计数 + 最近决策环，同一锁下取强一致快照）。 */
    public static ToolPolicyMatchStats stats() {
        synchronized (STATS_LOCK) {
            return new ToolPolicyMatchStats(exactHits, globHits, noneHits, List.copyOf(RECENT));
        }
    }

    /**
     * 进程级读面清零（测试隔离注入点——BuzhouMetricsHolder 进程态先例；生产勿调，
     * 清零后守恒不变量从新起点重计）。
     */
    public static void resetStats() {
        synchronized (STATS_LOCK) {
            exactHits = 0;
            globHits = 0;
            noneHits = 0;
            RECENT.clear();
        }
    }

    private static void record(ToolPolicyMatchDecision decision) {
        synchronized (STATS_LOCK) {
            switch (decision.outcome()) {
                case EXACT -> exactHits++;
                case GLOB -> globHits++;
                case NONE -> noneHits++;
            }
            RECENT.addFirst(decision);
            while (RECENT.size() > RECENT_CAPACITY) {
                RECENT.removeLast();
            }
        }
    }
}
