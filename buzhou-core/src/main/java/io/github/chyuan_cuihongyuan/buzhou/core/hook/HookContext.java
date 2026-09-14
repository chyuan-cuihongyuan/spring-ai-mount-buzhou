package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

/**
 * hook 回调上下文基接口——会话/代理标识、轮次号、状态 CAS 访问与事件外发。
 */
public interface HookContext {

    String sessionId();

    String agentName();

    int turn();

    SessionStateHandle state();

    void emitEvent(SessionEvent event);
}
