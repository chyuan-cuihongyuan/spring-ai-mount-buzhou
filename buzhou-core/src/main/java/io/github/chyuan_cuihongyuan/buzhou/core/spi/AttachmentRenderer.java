package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * Attachment 渲染桥接（spec 07 注入机制）。
 *
 * <p>guard（持 {@link FactStore}）实现此接口，memory 的注入视图构建方持有可选引用，
 * 在摘要块之后、近期原文之前插入事实 {@code <system-reminder>} 块。
 *
 * <p>返回 {@link java.util.Optional#empty()} 表示无事实可注入。
 */
@FunctionalInterface
public interface AttachmentRenderer {

    /**
     * 渲染当前轮次可注入的事实块文本（不含 {@code <system-reminder>} 包裹，由调用方包装）。
     *
     * @param sessionId   会话 id
     * @param currentTurn 当前轮次
     */
    java.util.Optional<String> render(String sessionId, int currentTurn);

    /**
     * 带注入上限的渲染（spec 07：{@code buzhou.facts.max-inject-chars} 总量约束，超出截断并附指针）。
     * 默认实现为纯文本截断；实现方宜覆写为按事实粒度截断并附被省略事实的 key 清单指针。
     *
     * @param maxChars 注入总字符上限；<=0 表示不限制
     */
    default java.util.Optional<String> render(String sessionId, int currentTurn, int maxChars) {
        java.util.Optional<String> text = render(sessionId, currentTurn);
        if (text.isEmpty() || maxChars <= 0 || text.get().length() <= maxChars) {
            return text;
        }
        return java.util.Optional.of(text.get().substring(0, maxChars));
    }
}
