package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionEventContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

public class DefaultSessionEventContext implements SessionEventContext {

    private final HookEnvironment env;
    private final SessionEvent event;

    public DefaultSessionEventContext(HookEnvironment env, SessionEvent event) {
        this.env = env;
        this.event = event;
    }

    @Override
    public SessionEvent event() {
        return event;
    }

    @Override
    public String sessionId() {
        return env.sessionId();
    }

    @Override
    public int turn() {
        return env.currentTurn();
    }

    @Override
    public SessionStateHandle state() {
        return env.stateHandle();
    }

    @Override
    public void emitEvent(SessionEvent event) {
        env.emit(event);
    }
}
