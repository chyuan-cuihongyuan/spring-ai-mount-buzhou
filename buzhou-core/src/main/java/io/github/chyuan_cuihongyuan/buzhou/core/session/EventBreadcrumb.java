package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.Objects;

/**
 * impl-759 / spec 1006：单条会话面包屑（Sentry breadcrumbs 借鉴——事件时间线
 * 尾部环，出事后看最后发生了什么）。只记类型与时刻，<b>不记 payload</b>
 * （事件载荷可能含会话内容——敏感红线）。
 *
 * @param epochMillis 事件交付时刻（{@code System.currentTimeMillis()}）
 * @param type        事件类型（如 {@code turn.feedback} / {@code session.forked}）
 */
public record EventBreadcrumb(long epochMillis, String type) {

    public EventBreadcrumb {
        Objects.requireNonNull(type, "type");
    }
}
