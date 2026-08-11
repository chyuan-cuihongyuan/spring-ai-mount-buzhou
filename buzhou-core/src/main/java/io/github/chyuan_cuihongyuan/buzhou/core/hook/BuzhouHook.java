package io.github.chyuan_cuihongyuan.buzhou.core.hook;

public interface BuzhouHook {

    default String name() {
        return getClass().getSimpleName();
    }

    default int order() {
        return 1000;
    }

    default HookResult beforeTurn(TurnContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult afterTurn(TurnContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult beforeModel(ModelCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult afterModel(ModelCallContext ctx) {
        return HookResult.CONTINUE;
    }

    /**
     * 模型调用终态失败（重试耗尽 / 命中不可重试类别 / 超时）后、决定兜底或放行之前触发（spec「onModelError 切面」）。
     *
     * <p>默认 {@link HookResult#CONTINUE}（放行——异常按底座原语义抛出），对既有 Hook 实现源码 / 二进制兼容。
     * 返回 {@link HookResult#replace(java.lang.Object) Replace(ChatClientResponse)} 可吞错并回填兜底响应；
     * 返回 {@link HookResult#block(String) Block(reason)} 回填文本兜底。失败原因经 {@link ModelCallContext#error()} 取得。
     */
    default HookResult onModelError(ModelCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult beforeTool(ToolCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default void onEvent(SessionEventContext ctx) {
    }
}
