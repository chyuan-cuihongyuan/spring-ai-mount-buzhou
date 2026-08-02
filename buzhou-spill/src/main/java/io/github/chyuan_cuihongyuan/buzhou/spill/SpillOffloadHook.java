package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.ToolPolicyMatcher;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

public class SpillOffloadHook implements BuzhouHook {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final int DEFAULT_THRESHOLD_CHARS = 32000;

    private final SpillService spillService;
    private final SessionReadOnlyRegistry readOnlyRegistry;
    private final Function<SpillUri, Path> spillFileResolver;
    private final int defaultThresholdChars;
    private final Map<String, Object> toolPolicies;

    public SpillOffloadHook(SpillService spillService, SessionReadOnlyRegistry readOnlyRegistry,
                            Function<SpillUri, Path> spillFileResolver,
                            int defaultThresholdChars, Map<String, Object> toolPolicies) {
        this.spillService = spillService;
        this.readOnlyRegistry = readOnlyRegistry;
        this.spillFileResolver = spillFileResolver;
        this.defaultThresholdChars = defaultThresholdChars <= 0 ? DEFAULT_THRESHOLD_CHARS : defaultThresholdChars;
        this.toolPolicies = toolPolicies == null ? Map.of() : toolPolicies;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        String result = String.valueOf(ctx.result());
        int threshold = thresholdFor(ctx.toolName());
        ArrayNode array = parseArray(result);
        if (array != null) {
            return offloadArrayItems(ctx, array, threshold);
        }
        SpillService.OffloadOutcome outcome = spillService.tryOffload(
                ctx.agentName(), ctx.sessionId(), ctx.toolCallId(), ctx.toolName(), result, threshold);
        if (outcome.degraded()) {
            emitDegraded(ctx, ctx.toolCallId(), outcome.uri());
            return HookResult.CONTINUE;
        }
        if (outcome.offloaded()) {
            registerReadOnly(ctx, outcome.uri());
            return HookResult.replace(outcome.text());
        }
        return HookResult.CONTINUE;
    }

    private HookResult offloadArrayItems(ToolCallContext ctx, ArrayNode array, int threshold) {
        boolean changed = false;
        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            String itemText = item.isTextual() ? item.asText() : item.toString();
            if (itemText.length() < threshold) {
                continue;
            }
            String itemCallId = ctx.toolCallId() + "-" + i;
            SpillService.OffloadOutcome outcome = spillService.tryOffload(
                    ctx.agentName(), ctx.sessionId(), itemCallId, ctx.toolName(), itemText, threshold);
            if (outcome.degraded()) {
                emitDegraded(ctx, itemCallId, outcome.uri());
                continue;
            }
            if (outcome.offloaded()) {
                registerReadOnly(ctx, outcome.uri());
                array.set(i, new com.fasterxml.jackson.databind.node.TextNode(outcome.text()));
                changed = true;
            }
        }
        if (!changed) {
            return HookResult.CONTINUE;
        }
        try {
            return HookResult.replace(MAPPER.writeValueAsString(array));
        } catch (Exception e) {
            return HookResult.CONTINUE;
        }
    }

    private ArrayNode parseArray(String result) {
        String trimmed = result.stripLeading();
        if (!trimmed.startsWith("[")) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(trimmed);
            return node instanceof ArrayNode array ? array : null;
        } catch (Exception e) {
            return null;
        }
    }

    private int thresholdFor(String toolName) {
        Map<String, Object> policy = ToolPolicyMatcher.match(toolPolicies, toolName);
        Object value = policy.get("spillThresholdChars");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultThresholdChars;
    }

    private void registerReadOnly(ToolCallContext ctx, SpillUri uri) {
        try {
            Path file = spillFileResolver.apply(uri);
            if (file != null) {
                readOnlyRegistry.register(ctx.sessionId(), file);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void emitDegraded(ToolCallContext ctx, String toolCallId, SpillUri uri) {
        ctx.emitEvent(new SessionEvent("offload.degraded",
                Map.of("toolName", ctx.toolName(),
                        "toolCallId", toolCallId,
                        "spillUri", uri == null ? "" : uri.toString()),
                Instant.now()));
    }
}
