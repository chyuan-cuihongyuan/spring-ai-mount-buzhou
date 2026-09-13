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
    /** impl-779 / spec 1026：注入覆盖计数（渲染/注入/省略——max-inject-chars 配置水位）。 */
    private final java.util.concurrent.atomic.AtomicLong renders =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong factsInjected =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong factsOmitted =
            new java.util.concurrent.atomic.AtomicLong();

    public FactAttachmentRenderer(FactStore factStore, List<FactDefinition> definitions) {
        this.factStore = factStore;
        this.definitions = definitions == null ? List.of() : definitions;
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn) {
        // 无上限语义 = 三参版 MAX_VALUE 收敛（输出恒等）；计数在读数版单点累计
        return render(sessionId, currentTurn, Integer.MAX_VALUE);
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
        int injected = 0;
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
            injected++;
        }
        if (!omittedKeys.isEmpty()) {
            factsOmitted.addAndGet(omittedKeys.size()); // spec 1026：省略水位
            sb.append("\n[更多事实未注入（超出 max-inject-chars）：")
                    .append(String.join(", ", omittedKeys)).append("]");
        }
        if (sb.isEmpty()) {
            return Optional.empty();
        }
        renders.incrementAndGet(); // spec 1026：非空产出渲染计数
        factsInjected.addAndGet(injected);
        return Optional.of(sb.toString());
    }

    /** 注入覆盖只读快照（spec 1026）。 */
    public FactInjectStats stats() {
        return new FactInjectStats(renders.get(), factsInjected.get(), factsOmitted.get());
    }

    /** 注入覆盖计数行（不可变）。 */
    public record FactInjectStats(long renders, long factsInjected, long factsOmitted) {
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
