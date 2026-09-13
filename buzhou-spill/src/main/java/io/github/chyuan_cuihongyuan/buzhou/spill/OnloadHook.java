package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OnloadHook implements BuzhouHook {

    private final FileSandbox sandbox;
    private final Map<String, List<LongContentParamPair>> longContentParams;
    /** impl-761 / spec 1008：回读三计数（守恒：attempts == loaded + failed——命中率 = loaded/attempts）。 */
    private final java.util.concurrent.atomic.AtomicLong attempts =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong loaded =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong failed =
            new java.util.concurrent.atomic.AtomicLong();

    public OnloadHook(FileSandbox sandbox, Map<String, List<LongContentParamPair>> longContentParams) {
        this.sandbox = sandbox;
        this.longContentParams = longContentParams == null ? Map.of() : longContentParams;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        List<LongContentParamPair> pairs = longContentParams.get(ctx.toolName());
        if (pairs == null || pairs.isEmpty()) {
            return HookResult.CONTINUE;
        }
        Map<String, Object> args = ctx.arguments();
        Map<String, Object> newArgs = null;
        for (LongContentParamPair pair : pairs) {
            Object raw = args.get(pair.pathParam());
            if (raw == null || raw.toString().isBlank()) {
                continue;
            }
            String content;
            attempts.incrementAndGet(); // spec 1008：每个非空 path 参数计一次回读尝试
            try {
                Path path = sandbox.resolve(raw.toString());
                content = Files.readString(path);
                if (content.isEmpty()) {
                    throw new OnloadException("加载内容为空：" + path);
                }
            } catch (Exception e) {
                failed.incrementAndGet();
                emitFailed(ctx, pair, raw.toString(), e);
                return HookResult.block("写侧加载失败（" + pair.pathParam() + "）：" + e.getMessage()
                        + "。请修正路径后重试。");
            }
            loaded.incrementAndGet();
            if (newArgs == null) {
                newArgs = new LinkedHashMap<>(args);
            }
            newArgs.put(pair.contentParam(), content);
            newArgs.remove(pair.pathParam());
        }
        return newArgs == null ? HookResult.CONTINUE : HookResult.replace(newArgs);
    }

    /** 回读命中率只读快照（守恒：attempts == loaded + failed——spec 1008）。 */
    public SpillOnloadStats stats() {
        return new SpillOnloadStats(attempts.get(), loaded.get(), failed.get());
    }

    private void emitFailed(ToolCallContext ctx, LongContentParamPair pair, String rawPath, Exception e) {
        // onFail 动词汇（T19）：写侧失败恒为 EXCEPTION（阻断，不外流残缺产物）——既有语义、词汇化标注
        ctx.emitEvent(new SessionEvent("onload.failed",
                Map.of("toolName", ctx.toolName(),
                        "toolCallId", ctx.toolCallId(),
                        "pathParam", pair.pathParam(),
                        "path", rawPath,
                        "reason", String.valueOf(e.getMessage()),
                        "onFail", "EXCEPTION"),
                Instant.now()));
    }

    static class OnloadException extends RuntimeException {
        OnloadException(String message) {
            super(message);
        }
    }
}
