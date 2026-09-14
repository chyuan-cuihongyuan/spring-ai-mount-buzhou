package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

/**
 * 事件通知切面上下文——hook 的 onEvent 回调所见（event() 载荷）。
 */
public interface SessionEventContext extends HookContext {

    SessionEvent event();
}
