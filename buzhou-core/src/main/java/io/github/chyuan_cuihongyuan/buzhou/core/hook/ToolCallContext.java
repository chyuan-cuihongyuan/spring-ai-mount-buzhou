package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.Map;

/**
 * 工具调用切面上下文——beforeTool/afterTool 所见（arguments/result 可改写）。
 */
public interface ToolCallContext extends HookContext {

    String toolCallId();

    String toolName();

    Map<String, Object> arguments();

    Object result();

    Throwable error();

    void replaceArguments(Map<String, Object> newArguments);

    void replaceResult(Object newResult);
}
