package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 组合式 Attachment 渲染器（spec 07 注入机制 / ticket 22 装配层）。
 *
 * <p>多个事实/状态渲染器并存时（如 guard 的 {@code FactAttachmentRenderer} 与 tools 的
 * {@code TodoAttachmentRenderer}）由装配侧用本类组合：按顺序收集各子渲染器的文本，
 * 非空者以空行分隔拼接后整体返回。任一非空即返回 {@link Optional#of}，全空返回
 * {@link Optional#empty()}。
 *
 * <p>{@link #render(String, int, int)} 的总量上限（{@code buzhou.facts.max-inject-chars}）
 * 作用在拼接后的整体文本上（末端截断），与单渲染器的「按事实粒度截断」语义相比是粗粒度兜底；
 * 装配期只有多渲染器并存时才走本路径，单渲染器仍直接使用原实现。
 */
public class CompositeAttachmentRenderer implements AttachmentRenderer {

    private final List<AttachmentRenderer> renderers;

    public CompositeAttachmentRenderer(List<AttachmentRenderer> renderers) {
        this.renderers = renderers == null ? List.of() : List.copyOf(renderers);
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn) {
        return join(renderers.stream()
                .map(r -> r.render(sessionId, currentTurn))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList());
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn, int maxChars) {
        String joined = render(sessionId, currentTurn).orElse(null);
        if (joined == null) {
            return Optional.empty();
        }
        if (maxChars <= 0 || joined.length() <= maxChars) {
            return Optional.of(joined);
        }
        return Optional.of(joined.substring(0, maxChars));
    }

    private static Optional<String> join(List<String> parts) {
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parts.stream().collect(Collectors.joining("\n\n")));
    }
}
