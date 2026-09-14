package io.github.chyuan_cuihongyuan.buzhou.core.hook;
/**
 * hook 裁决结果 sealed 接口——CONTINUE 放行 / Block 阻断（reason 即最终回复）/
 * Replace 替换载荷（类型不匹配静默跳过并计数，spec 1013）。
 */
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
