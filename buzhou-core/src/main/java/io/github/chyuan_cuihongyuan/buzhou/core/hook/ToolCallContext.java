package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.Map;

public interface ToolCallContext extends HookContext {

    String toolCallId();

    String toolName();

    Map<String, Object> arguments();

    Object result();

    Throwable error();

    void replaceArguments(Map<String, Object> newArguments);

    void replaceResult(Object newResult);
}
