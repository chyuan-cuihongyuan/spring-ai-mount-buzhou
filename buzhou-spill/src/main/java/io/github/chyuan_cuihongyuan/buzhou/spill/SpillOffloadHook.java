package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.OnFail;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
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

    /** 读侧 onFail=REFRAIN 时的保守降级文案（不给可能残缺的数据，让模型可重试）。 */
    static final String REFRAIN_NOTICE = "[读侧护栏降级（onFail=REFRAIN）]：该工具结果因溢出组件故障"
            + "无法安全注入上下文，已保守拒答该数据；可重新调用该工具获取完整结果。";

    private final SpillService spillService;
    private final SessionReadOnlyRegistry readOnlyRegistry;
    private final Function<SpillUri, Path> spillFileResolver;
    private final int defaultThresholdChars;
    private final Map<String, Object> toolPolicies;
    private final OnFail onFail;

    public SpillOffloadHook(SpillService spillService, SessionReadOnlyRegistry readOnlyRegistry,
                            Function<SpillUri, Path> spillFileResolver,
                            int defaultThresholdChars, Map<String, Object> toolPolicies) {
        this(spillService, readOnlyRegistry, spillFileResolver, defaultThresholdChars, toolPolicies,
                OnFail.FILTER);
    }

    public SpillOffloadHook(SpillService spillService, SessionReadOnlyRegistry readOnlyRegistry,
                            Function<SpillUri, Path> spillFileResolver,
                            int defaultThresholdChars, Map<String, Object> toolPolicies,
                            OnFail onFail) {
        this.spillService = spillService;
        this.readOnlyRegistry = readOnlyRegistry;
        this.spillFileResolver = spillFileResolver;
        this.defaultThresholdChars = defaultThresholdChars <= 0 ? DEFAULT_THRESHOLD_CHARS : defaultThresholdChars;
        this.toolPolicies = toolPolicies == null ? Map.of() : toolPolicies;
        this.onFail = onFail == null ? OnFail.FILTER : onFail;
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
        if (SpillThresholds.isDurable(toolPolicies, ctx.toolName())) {
            return HookResult.CONTINUE; // T22 durable 覆盖：声明「永不溢出」的输出保持全量内联
        }
        String raw = String.valueOf(ctx.result());
        // guard 注入防御（spotlighting）开启时先解包裹还原原文：形状识别/阈值/落盘均按干净原文，
        // SpillStore 存无标记污染的原文（回读质量）；未开启时 unwrap 原样返回。
        boolean wasWrapped = raw.contains(Spotlighting.BEGIN_HEAD);
        String effective = Spotlighting.unwrap(raw);
        int threshold = SpillThresholds.thresholdFor(toolPolicies, ctx.toolName(), defaultThresholdChars);
        ArrayNode array = parseArray(effective);
        if (array != null) {
            return offloadArrayItems(ctx, array, threshold, wasWrapped);
        }
        SpillService.OffloadOutcome outcome = spillService.tryOffload(
                ctx.agentName(), ctx.sessionId(), ctx.toolCallId(), ctx.toolName(), effective, threshold);
        if (outcome.degraded()) {
            emitDegraded(ctx, ctx.toolCallId(), outcome.uri());
            // onFail 动词汇（T19）：FILTER=透传原文（既有降级语义）；REFRAIN=保守拒答替代
            if (onFail == OnFail.REFRAIN) {
                return HookResult.replace(REFRAIN_NOTICE);
            }
            return HookResult.CONTINUE;
        }
        if (outcome.offloaded()) {
            registerReadOnly(ctx, outcome.uri());
            return HookResult.replace(outcome.text());
        }
        return HookResult.CONTINUE;
    }

    private HookResult offloadArrayItems(ToolCallContext ctx, ArrayNode array, int threshold, boolean wasWrapped) {
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
            String json = MAPPER.writeValueAsString(array);
            // 原内容若已被 spotlight 包裹：替换结果保持包裹（剩余内联项不脱防）
            return HookResult.replace(wasWrapped
                    ? Spotlighting.wrap("respill", Spotlighting.DEFAULT_MARK_CHAR, 1, json)
                    : json);
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
        // impl-41 / spec 13 §T66：spill 指标（outcome=degraded——原文回喂）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.spill.requests", "outcome", "degraded");
        ctx.emitEvent(new SessionEvent("offload.degraded",
                Map.of("toolName", ctx.toolName(),
                        "toolCallId", toolCallId,
                        "spillUri", uri == null ? "" : uri.toString(),
                        "onFail", onFail.name()),
                Instant.now()));
    }
}
