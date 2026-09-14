package io.github.chyuan_cuihongyuan.buzhou.core.session;
/**
 * 会话事件监听者（addEventListener 注册；事件分发逐 listener 异常隔离，
 * 单个监听者崩溃不阻断其余——impl-30 / spec 13 §core-1）。
 */


@FunctionalInterface
public interface SessionEventListener {

    void onEvent(SessionEvent event);
}
