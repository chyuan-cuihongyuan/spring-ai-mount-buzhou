package io.github.chyuan_cuihongyuan.buzhou.core.hook;

public interface TurnContext extends HookContext {

    String input();

    String response();

    void replaceInput(String newInput);

    void replaceResponse(String newResponse);
}
