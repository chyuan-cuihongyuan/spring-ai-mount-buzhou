package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBreadcrumb;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * impl-759 / spec 1006：会话面包屑有界环（Sentry breadcrumbs 借鉴）——
 * 双模式共同漏斗 {@code DefaultAgentSession.deliverEvent} 单点记录，
 * 只记事件类型不记 payload（敏感红线）。会话域实例（非进程级）。
 */
final class BreadcrumbRing {

    /** 环容量（有界纪律；Sentry 默认 100，进程内会话数多取轻量 32）。 */
    static final int CAPACITY = 32;

    private final Deque<EventBreadcrumb> ring = new ArrayDeque<>();

    /** 记录一条已交付事件（新→旧，超限挤掉最旧）。 */
    synchronized void record(String type) {
        ring.addFirst(new EventBreadcrumb(System.currentTimeMillis(), type));
        while (ring.size() > CAPACITY) {
            ring.removeLast();
        }
    }

    /** 只读快照（新→旧；不可变）。 */
    synchronized List<EventBreadcrumb> snapshot() {
        return List.copyOf(ring);
    }

    /** 清空（会话关闭清理路径用）。 */
    synchronized void clear() {
        ring.clear();
    }
}
