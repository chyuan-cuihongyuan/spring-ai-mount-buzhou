package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OnloadHookTest {

    @TempDir
    Path root;

    @TempDir
    Path outside;

    private final List<SessionEvent> events = new ArrayList<>();

    private OnloadHook hook(Map<String, List<LongContentParamPair>> params) {
        return new OnloadHook(new FileSandbox(root, List.of()), params);
    }

    private ToolCallContext ctx(String toolName, Map<String, Object> args) {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        env.bindEventPublisher(events::add);
        return new DefaultToolCallContext(env, "tc-1", toolName, args);
    }

    @Test
    void orderIsBuiltinBeforeToolSlot() {
        assertThat(hook(Map.of()).order()).isEqualTo(200);
    }

    @Test
    void toolWithoutDeclaredPairsPassesThrough() {
        ToolCallContext ctx = ctx("unknown_tool", Map.of("anythingPath", "x"));
        assertThat(hook(Map.of()).beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }

    @Test
    void blankPathParamPassesThroughDirectContent() {
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        ToolCallContext ctx = ctx("write_file", Map.of("content", "inline", "contentPath", ""));
        assertThat(hook.beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }

    @Test
    void pathParamLoadsFullContentAndIsStripped() throws Exception {
        Path script = root.resolve("task.etl");
        Files.writeString(script, "全文内容".repeat(100));
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        Map<String, Object> args = new HashMap<>();
        args.put("path", "out.txt");
        args.put("contentPath", script.toString());
        ToolCallContext ctx = ctx("write_file", args);

        HookResult result = hook.beforeTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> newArgs = (Map<String, Object>) ((HookResult.Replace) result).payload();
        assertThat(newArgs.get("content")).isEqualTo("全文内容".repeat(100));
        assertThat(newArgs).doesNotContainKey("contentPath");
        assertThat(newArgs.get("path")).isEqualTo("out.txt");
    }

    @Test
    void pathOutsideWhitelistBlocksAndEmitsErrorEvent() throws Exception {
        Path secret = outside.resolve("secret.txt");
        Files.writeString(secret, "secret");
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        ToolCallContext ctx = ctx("write_file", Map.of("contentPath", secret.toString()));

        HookResult result = hook.beforeTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason()).contains("contentPath");
        assertThat(events).anyMatch(e -> e.type().equals("onload.failed"));
    }

    @Test
    void missingFileBlocks() {
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        ToolCallContext ctx = ctx("write_file",
                Map.of("contentPath", root.resolve("nope.txt").toString()));

        HookResult result = hook.beforeTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(events).anyMatch(e -> e.type().equals("onload.failed"));
    }

    @Test
    void emptyContentBlocks() throws Exception {
        Path empty = root.resolve("empty.txt");
        Files.writeString(empty, "");
        OnloadHook hook = hook(Map.of("write_file",
                List.of(new LongContentParamPair("content", "contentPath"))));
        ToolCallContext ctx = ctx("write_file", Map.of("contentPath", empty.toString()));

        assertThat(hook.beforeTool(ctx)).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void multiplePairsEachLoadIndependently() throws Exception {
        Path a = root.resolve("a.txt");
        Path b = root.resolve("b.txt");
        Files.writeString(a, "AAA");
        Files.writeString(b, "BBB");
        OnloadHook hook = hook(Map.of("dual", List.of(
                new LongContentParamPair("content", "contentPath"),
                new LongContentParamPair("newStr", "newStrPath"))));
        Map<String, Object> args = new HashMap<>();
        args.put("contentPath", a.toString());
        args.put("newStrPath", b.toString());
        ToolCallContext ctx = ctx("dual", args);

        HookResult result = hook.beforeTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> newArgs = (Map<String, Object>) ((HookResult.Replace) result).payload();
        assertThat(newArgs.get("content")).isEqualTo("AAA");
        assertThat(newArgs.get("newStr")).isEqualTo("BBB");
        assertThat(newArgs).doesNotContainKeys("contentPath", "newStrPath");
    }
}
