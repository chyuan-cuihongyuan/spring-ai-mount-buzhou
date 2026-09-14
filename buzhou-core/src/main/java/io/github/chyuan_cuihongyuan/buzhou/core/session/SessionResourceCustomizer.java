package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionResourceRegistry;

/**
 * 会话资源定制者——spawn 期向会话资源注册表挂 close 钩子（LIFO 逆序关闭，
 * 会话 close 时统一排空）。
 */


@FunctionalInterface
public interface SessionResourceCustomizer {

    void customize(SessionResourceRegistry registry, String appId, String agentName, String sessionId);
}
