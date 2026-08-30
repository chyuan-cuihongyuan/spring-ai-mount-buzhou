package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 事件 schema 检查器（spec 209 / T583，JSON Schema required 最小面）：
 * per-type 必备键集——缺键事件默认<b>丢弃</b>（fail-closed 保护下游）+
 * violated 计数；未声明类型放行（open-world）；failOpen=true 观察模式违规
 * 可见但不拦（调查期）。事件契约双层：信封（20）+ payload 必备键（本类）。
 */
public final class EventSchemaChecker implements SessionEventListener {

    private static final String VIOLATED_COUNTER = "buzhou.event.schema-violated";

    private final SessionEventListener delegate;
    private final Map<String, Set<String>> requiredKeysByType;
    private final boolean failOpen;

    public EventSchemaChecker(SessionEventListener delegate,
                              Map<String, Set<String>> requiredKeysByType) {
        this(delegate, requiredKeysByType, false);
    }

    public EventSchemaChecker(SessionEventListener delegate,
                              Map<String, Set<String>> requiredKeysByType, boolean failOpen) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 非空");
        }
        this.delegate = delegate;
        this.requiredKeysByType = requiredKeysByType == null
                ? Map.of() : Map.copyOf(requiredKeysByType);
        this.failOpen = failOpen;
    }

    /** 违规键列表（缺哪些必备键；未声明类型 = 空）。 */
    public List<String> violations(SessionEvent event) {
        if (event == null) {
            return List.of();
        }
        Set<String> required = requiredKeysByType.get(event.type());
        if (required == null || required.isEmpty()) {
            return List.of(); // open-world / 空键集
        }
        return required.stream()
                .filter(key -> !event.payload().containsKey(key))
                .sorted()
                .toList();
    }

    @Override
    public void onEvent(SessionEvent event) {
        List<String> violations = violations(event);
        if (!violations.isEmpty()) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter(VIOLATED_COUNTER, 1);
            if (!failOpen) {
                return; // fail-closed：坏事件不出门
            }
        }
        delegate.onEvent(event);
    }
}
