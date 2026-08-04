package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 事实 Attachment 渲染器（spec 07 注入机制）。
 *
 * <p>guard 实现 {@link AttachmentRenderer}：扫描 {@link FactStore#activeFacts} 未过期事实 →
 * 逐条 {@link FactDefinition#render} → 合并为单段文本（不含 {@code <system-reminder>} 包裹，
 * 由 memory 的注入视图构建方包装成块）。
 */
public class FactAttachmentRenderer implements AttachmentRenderer {

    private final FactStore factStore;
    private final List<FactDefinition> definitions;

    public FactAttachmentRenderer(FactStore factStore, List<FactDefinition> definitions) {
        this.factStore = factStore;
        this.definitions = definitions == null ? List.of() : definitions;
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn) {
        List<Fact> active = factStore.activeFacts(sessionId, currentTurn);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        String text = active.stream()
                .map(this::renderFact)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    /**
     * 按事实粒度截断（spec 07：max-inject-chars 总量约束）：逐条累积至上限，
     * 被省略事实以其 {@code fact.{producer}.{name}} key 清单作指针附尾（仅供排障核对，
     * 事实无模型侧回读工具）。
     */
    @Override
    public Optional<String> render(String sessionId, int currentTurn, int maxChars) {
        if (maxChars <= 0) {
            return render(sessionId, currentTurn);
        }
        List<Fact> active = factStore.activeFacts(sessionId, currentTurn);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        List<String> omittedKeys = new java.util.ArrayList<>();
        for (Fact fact : active) {
            String line = renderFact(fact);
            if (line == null || line.isBlank()) {
                continue;
            }
            if (sb.length() + line.length() + 1 > maxChars) {
                omittedKeys.add(fact.key());
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        if (!omittedKeys.isEmpty()) {
            sb.append("\n[更多事实未注入（超出 max-inject-chars）：")
                    .append(String.join(", ", omittedKeys)).append("]");
        }
        return sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
    }

    private String renderFact(Fact fact) {
        // 按 producer 匹配 FactDefinition 的 render；找不到则用通用渲染
        return definitions.stream()
                .filter(d -> d.name().equals(fact.producer()))
                .findFirst()
                .map(d -> d.render(fact))
                .orElseGet(() -> "- " + fact.producer() + ": " + fact.value());
    }
}
