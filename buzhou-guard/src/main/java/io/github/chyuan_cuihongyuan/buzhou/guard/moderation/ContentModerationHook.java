package io.github.chyuan_cuihongyuan.buzhou.guard.moderation;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 内容安全词表过滤（spec 515 / T781，OpenAI moderation 本地词表面思想）：
 * 宿主声明违禁词（大小写不敏感 contains——CJK 无词界 plain contains 正确
 * 语义，无正则无 ReDoS），双缝处理——用户输入缝（beforeTurn）与工具结果缝
 * （afterTool）；动作 BLOCK（结构化告示）或 MASK（命中段替换 [已屏蔽]）。
 *
 * <p>诚实边界：contains 级召回（变体/形近字归 ML 面扩散）；告示为可信
 * 框架文本（CanaryGuard INTERCEPT_NOTICE 同族）；MASK 不回显命中词
 * （回显即二次传播）。
 */
public class ContentModerationHook implements BuzhouHook {

    public static final int ORDER = 210;
    /** MASK 动作占位（不回显命中词）。 */
    public static final String MASK_PLACEHOLDER = "[已屏蔽]";

    /** 处理动作。 */
    public enum Action { BLOCK, MASK }

    /** 工具缝拦截告示（可信框架文本——CanaryGuard 告示同族）。 */
    public static final String TOOL_BLOCK_NOTICE =
            "[该工具结果已拦截：命中内容安全词表。该结果已丢弃；请调整查询或换用其他数据来源。]";

    /** 输入缝拦截告示。 */
    public static final String INPUT_BLOCK_NOTICE =
            "[该输入已拦截：命中内容安全词表。请修改表述后重试。]";

    private final List<String> terms;
    private final Action action;

    public ContentModerationHook(List<String> terms, Action action) {
        if (terms == null || terms.isEmpty()) {
            throw new IllegalArgumentException("内容安全词表非空（空表请不装配）");
        }
        List<String> normalized = new ArrayList<>();
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                throw new IllegalArgumentException("违禁词非空白");
            }
            normalized.add(term.toLowerCase(Locale.ROOT));
        }
        this.terms = List.copyOf(normalized);
        this.action = action == null ? Action.BLOCK : action;
    }

    @Override
    public String name() {
        return "ContentModerationHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        String input = ctx == null ? null : ctx.input();
        List<String> hits = hitsIn(input);
        if (hits.isEmpty()) {
            return HookResult.CONTINUE;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.guard.moderation.hits",
                "seam", "input");
        if (action == Action.BLOCK) {
            return HookResult.block(INPUT_BLOCK_NOTICE);
        }
        String masked = mask(input);
        ctx.replaceInput(masked);
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx == null || ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        String content = String.valueOf(ctx.result());
        List<String> hits = hitsIn(content);
        if (hits.isEmpty()) {
            return HookResult.CONTINUE;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.guard.moderation.hits",
                "seam", "tool-output");
        if (action == Action.BLOCK) {
            ctx.replaceResult(TOOL_BLOCK_NOTICE);
            return HookResult.CONTINUE;
        }
        ctx.replaceResult(mask(content));
        return HookResult.CONTINUE;
    }

    /** 命中词列表（大小写不敏感 contains；空文本返回空）。 */
    private List<String> hitsIn(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (String term : terms) {
            if (lower.contains(term)) {
                hits.add(term);
            }
        }
        return hits;
    }

    /** MASK：所有命中段（各词所有出现）替换 [已屏蔽]。 */
    private String mask(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(text);
        int offset = 0;
        for (String term : terms) {
            int idx = lower.indexOf(term, offset);
            while (idx >= 0) {
                out.replace(idx, idx + term.length(), MASK_PLACEHOLDER);
                offset = idx + MASK_PLACEHOLDER.length();
                // lower 与 out 长度已错位——重建 lower 相对偏移：从 offset 起重新计算
                lower = out.toString().toLowerCase(Locale.ROOT);
                idx = lower.indexOf(term, offset);
            }
        }
        return out.toString();
    }
}
