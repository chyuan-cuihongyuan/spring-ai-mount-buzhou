package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.ToolPolicyMatcher;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MemoryModule {

    private MemoryModule() {
    }

    public static RuntimeConfig configure(Map<String, Object> ymlConfig, MessageStore messageStore) {
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        Function<String, MicroCompactionPolicy> policyFn = policyFn(ymlConfig);
        int protectRecentTurns = protectRecentTurns(ymlConfig);

        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                (sessionId, stored, currentTurn) ->
                        compactor.compact(stored, currentTurn, policyFn, protectRecentTurns)
                                .compactedView(),
                List.of(new EvidenceLookupTool(messageStore)));
    }

    @SuppressWarnings("unchecked")
    private static Function<String, MicroCompactionPolicy> policyFn(Map<String, Object> ymlConfig) {
        Object section = ymlConfig.get("tool-policies");
        Map<String, Object> toolPolicies = section instanceof Map
                ? (Map<String, Object>) section : Map.of();
        return toolName -> {
            Map<String, Object> matched = ToolPolicyMatcher.match(toolPolicies, toolName);
            Object mc = matched.get("micro-compaction");
            if (!(mc instanceof Map)) {
                return MicroCompactionPolicy.defaults();
            }
            Map<String, Object> mcMap = (Map<String, Object>) mc;
            return new MicroCompactionPolicy(
                    bool(mcMap.get("never-compress"), false),
                    integer(mcMap.get("max-age-turns"), 3),
                    integer(mcMap.get("min-size-chars"), 200));
        };
    }

    @SuppressWarnings("unchecked")
    private static int protectRecentTurns(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object mc = ((Map<String, Object>) memory).get("micro-compaction");
            if (mc instanceof Map) {
                return integer(((Map<String, Object>) mc).get("protect-recent-turns"), 1);
            }
        }
        return 1;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean b ? b : fallback;
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }
}
