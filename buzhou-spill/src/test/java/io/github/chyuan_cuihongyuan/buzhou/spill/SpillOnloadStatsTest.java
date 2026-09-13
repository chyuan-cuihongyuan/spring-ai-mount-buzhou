package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpillOnloadStatsTest {

    @TempDir
    Path root;

    private OnloadHook hook(Map<String, List<LongContentParamPair>> params) {
        return new OnloadHook(new FileSandbox(root, List.of()), params);
    }

    private ToolCallContext ctx(String toolName, Map<String, Object> args) {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        env.bindEventPublisher(events -> { });
        return new DefaultToolCallContext(env, "tc-1", toolName, args);
    }

    @Test
    void successfulOnloadCountsAttemptAndLoaded() throws Exception {
        Path script = root.resolve("task.etl");
        Files.writeString(script, "全文内容".repeat(10));
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        Map<String, Object> args = new HashMap<>();
        args.put("path", "out.txt");
        args.put("contentPath", script.toString());

        HookResult result = hook.beforeTool(ctx("write_file", args));

        assertThat(result).isNotSameAs(HookResult.CONTINUE);
        SpillOnloadStats stats = hook.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.loaded()).isEqualTo(1);
        assertThat(stats.failed()).isZero();
    }

    @Test
    void failedOnloadCountsAttemptAndFailed() {
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        Map<String, Object> args = new HashMap<>();
        args.put("path", "out.txt");
        args.put("contentPath", root.resolve("missing.etl").toString());

        HookResult result = hook.beforeTool(ctx("write_file", args));

        assertThat(result).isNotSameAs(HookResult.CONTINUE);
        SpillOnloadStats stats = hook.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.failed()).isEqualTo(1);
        assertThat(stats.loaded()).isZero();
    }

    @Test
    void blankPathIsNotAnAttempt() {
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        Map<String, Object> args = new HashMap<>();
        args.put("path", "out.txt");
        args.put("contentPath", "");

        assertThat(hook.beforeTool(ctx("write_file", args))).isSameAs(HookResult.CONTINUE);
        assertThat(hook.stats().attempts()).isZero();
    }

    @Test
    void conservationHoldsAcrossMixedOutcomes() throws Exception {
        Path ok = root.resolve("ok.etl");
        Files.writeString(ok, "内容");
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));

        Map<String, Object> hit = new HashMap<>();
        hit.put("path", "a");
        hit.put("contentPath", ok.toString());
        hook.beforeTool(ctx("write_file", hit));

        Map<String, Object> miss = new HashMap<>();
        miss.put("path", "b");
        miss.put("contentPath", root.resolve("nope.etl").toString());
        hook.beforeTool(ctx("write_file", miss));

        Map<String, Object> hit2 = new HashMap<>();
        hit2.put("path", "c");
        hit2.put("contentPath", ok.toString());
        hook.beforeTool(ctx("write_file", hit2));

        SpillOnloadStats stats = hook.stats();
        assertThat(stats.attempts()).isEqualTo(3);
        assertThat(stats.loaded() + stats.failed()).isEqualTo(stats.attempts());
        assertThat(stats.loaded()).isEqualTo(2);
        assertThat(stats.failed()).isEqualTo(1);
    }
}
