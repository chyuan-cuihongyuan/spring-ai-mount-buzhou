package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CopyOnWriteGuardHook implements BuzhouHook {

    private static final Map<String, String> DEFAULT_EDIT_TOOLS = Map.of("str_replace", "path");

    private final SessionReadOnlyRegistry readOnlyRegistry;
    private final List<Path> readonlyRoots;
    private final Map<String, String> editToolPathParams;

    public CopyOnWriteGuardHook(SessionReadOnlyRegistry readOnlyRegistry, List<Path> readonlyRoots) {
        this(readOnlyRegistry, readonlyRoots, DEFAULT_EDIT_TOOLS);
    }

    public CopyOnWriteGuardHook(SessionReadOnlyRegistry readOnlyRegistry, List<Path> readonlyRoots,
                                Map<String, String> editToolPathParams) {
        this.readOnlyRegistry = readOnlyRegistry;
        this.readonlyRoots = (readonlyRoots == null ? List.<Path>of() : readonlyRoots).stream()
                .map(SessionReadOnlyRegistry::normalize).toList();
        this.editToolPathParams = editToolPathParams == null ? DEFAULT_EDIT_TOOLS : editToolPathParams;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        String pathParam = editToolPathParams.get(ctx.toolName());
        if (pathParam == null) {
            return HookResult.CONTINUE;
        }
        Object raw = ctx.arguments().get(pathParam);
        if (raw == null || raw.toString().isBlank()) {
            return HookResult.CONTINUE;
        }
        Path target = SessionReadOnlyRegistry.normalize(Path.of(raw.toString()));
        boolean readOnly = readOnlyRegistry.isReadOnly(ctx.sessionId(), target)
                || readonlyRoots.stream().anyMatch(target::startsWith);
        if (!readOnly) {
            return HookResult.CONTINUE;
        }
        ctx.emitEvent(new SessionEvent("guard.tool.blocked",
                Map.of("toolName", ctx.toolName(),
                        "toolCallId", ctx.toolCallId(),
                        "guard", "copy-on-write",
                        "path", target.toString()),
                Instant.now()));
        return HookResult.block("目标为只读快照或只读区，不能直接编辑：" + target
                + "。请先 copy_file(srcPath=\"" + target + "\", destPath=\"<工作区路径>\") 生成工作副本，再对副本编辑。");
    }
}
