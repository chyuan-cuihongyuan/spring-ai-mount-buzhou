package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

public class DefaultTurnContext implements TurnContext {

    private final HookEnvironment env;
    private String input;
    private String response;

    public DefaultTurnContext(HookEnvironment env, String input) {
        this.env = env;
        this.input = input;
    }

    @Override
    public String sessionId() {
        return env.sessionId();
    }

    @Override
    public String agentName() {
        return env.agentName();
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

    @Override
    public String input() {
        return input;
    }

    @Override
    public String response() {
        return response;
    }

    public void markResponded(String response) {
        this.response = response;
    }

    @Override
    public void replaceInput(String newInput) {
        this.input = newInput;
    }

    @Override
    public void replaceResponse(String newResponse) {
        this.response = newResponse;
    }
}
