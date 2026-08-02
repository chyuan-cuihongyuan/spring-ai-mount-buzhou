package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.Map;

public class DefaultToolCallContext implements ToolCallContext {

    private final HookEnvironment env;
    private final String toolCallId;
    private final String toolName;
    private Map<String, Object> arguments;
    private Object result;
    private Throwable error;

    public DefaultToolCallContext(HookEnvironment env, String toolCallId, String toolName,
                                  Map<String, Object> arguments) {
        this.env = env;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.arguments = arguments;
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

    @Override
    public String toolCallId() {
        return toolCallId;
    }

    @Override
    public String toolName() {
        return toolName;
    }

    @Override
    public Map<String, Object> arguments() {
        return arguments;
    }

    @Override
    public Object result() {
        return result;
    }

    @Override
    public Throwable error() {
        return error;
    }

    public void markExecuted(Object result, Throwable error) {
        this.result = result;
        this.error = error;
    }

    @Override
    public void replaceArguments(Map<String, Object> newArguments) {
        this.arguments = newArguments;
    }

    @Override
    public void replaceResult(Object newResult) {
        this.result = newResult;
    }
}
