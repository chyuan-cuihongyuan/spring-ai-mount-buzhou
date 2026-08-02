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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CopyOnWriteGuardHookTest {

    @TempDir
    Path workRoot;

    @TempDir
    Path readonlyRoot;

    private final List<SessionEvent> events = new ArrayList<>();
    private SessionReadOnlyRegistry registry;
    private CopyOnWriteGuardHook hook;

    private void setup() {
        registry = new SessionReadOnlyRegistry();
        hook = new CopyOnWriteGuardHook(registry, List.of(readonlyRoot));
    }

    private ToolCallContext ctx(String sessionId, String toolName, Map<String, Object> args) {
        HookEnvironment env = new HookEnvironment(sessionId, "agent", new InMemorySessionStateStore());
        env.bindEventPublisher(events::add);
        return new DefaultToolCallContext(env, "tc-1", toolName, args);
    }

    @Test
    void orderIsFirstBeforeToolSlot() {
        setup();
        assertThat(hook.order()).isEqualTo(100);
    }

    @Test
    void nonEditToolPassesThrough() {
        setup();
        ToolCallContext ctx = ctx("s1", "read_file", Map.of("path", "x"));
        assertThat(hook.beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }

    @Test
    void editToolWithoutPathArgPassesThrough() {
        setup();
        ToolCallContext ctx = ctx("s1", "str_replace", Map.of());
        assertThat(hook.beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }

    @Test
    void directEditOfSessionReadOnlySnapshotIsBlocked() throws Exception {
        setup();
        Path snapshot = workRoot.resolve("snap.spill");
        Files.writeString(snapshot, "data");
        registry.register("s1", snapshot);
        ToolCallContext ctx = ctx("s1", "str_replace", Map.of("path", snapshot.toString()));

        HookResult result = hook.beforeTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason()).contains("copy_file");
        assertThat(events).anyMatch(e -> e.type().equals("guard.tool.blocked"));
    }

    @Test
    void directEditUnderReadonlyRootIsBlocked() throws Exception {
        setup();
        Path file = readonlyRoot.resolve("origin.txt");
        Files.writeString(file, "data");
        ToolCallContext ctx = ctx("s1", "str_replace", Map.of("path", file.toString()));

        assertThat(hook.beforeTool(ctx)).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void snapshotRegisteredForOtherSessionDoesNotBlock() throws Exception {
        setup();
        Path snapshot = workRoot.resolve("snap.spill");
        Files.writeString(snapshot, "data");
        registry.register("s-other", snapshot);
        ToolCallContext ctx = ctx("s1", "str_replace", Map.of("path", snapshot.toString()));

        assertThat(hook.beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }

    @Test
    void editOfWorkingCopyPassesThrough() throws Exception {
        setup();
        Path work = workRoot.resolve("work.txt");
        Files.writeString(work, "data");
        registry.register("s1", workRoot.resolve("snap.spill"));
        ToolCallContext ctx = ctx("s1", "str_replace", Map.of("path", work.toString()));

        assertThat(hook.beforeTool(ctx)).isSameAs(HookResult.CONTINUE);
    }
}
