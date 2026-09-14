package io.github.chyuan_cuihongyuan.buzhou.core.hook;
/**
 * 轮次切面上下文——beforeTurn/afterTurn 所见（input/response 可改写）。
 */
public interface TurnContext extends HookContext {

    String input();

    String response();

    void replaceInput(String newInput);

    void replaceResponse(String newResponse);
}
