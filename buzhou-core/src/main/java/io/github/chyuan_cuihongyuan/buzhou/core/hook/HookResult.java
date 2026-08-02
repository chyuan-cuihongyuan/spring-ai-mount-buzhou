package io.github.chyuan_cuihongyuan.buzhou.core.hook;

public sealed interface HookResult
        permits HookResult.Continue, HookResult.Block, HookResult.Replace {

    HookResult CONTINUE = new Continue();

    record Continue() implements HookResult {
    }

    record Block(String reason) implements HookResult {
    }

    record Replace(Object payload) implements HookResult {
    }

    static HookResult block(String reason) {
        return new Block(reason);
    }

    static HookResult replace(Object payload) {
        return new Replace(payload);
    }
}
