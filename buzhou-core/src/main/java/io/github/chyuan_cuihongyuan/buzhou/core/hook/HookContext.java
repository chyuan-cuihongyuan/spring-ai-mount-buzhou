package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

public interface HookContext {

    String sessionId();

    int turn();

    SessionStateHandle state();

    void emitEvent(SessionEvent event);
}
