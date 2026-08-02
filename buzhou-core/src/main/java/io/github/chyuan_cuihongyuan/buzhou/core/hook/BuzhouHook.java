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

    default HookResult beforeTool(ToolCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE;
    }

    default void onEvent(SessionEventContext ctx) {
    }
}
