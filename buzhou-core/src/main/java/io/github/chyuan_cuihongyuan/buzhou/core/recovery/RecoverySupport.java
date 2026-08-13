package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 恢复支持（wayfinder2 impl-06/07 / T32+T33 / docs/spec/12）：把 Run 注册表 +
 * 事件溯源工具调用日志挂进既有 {@link RuntimeConfig}——
 *
 * <ul>
 *   <li>{@link RunStateTrackerHook}：turn 边界持久化 run 快照（Completed-Turn 为快照单元）；</li>
 *   <li>装配 customizer：把 {@link ToolCallLog} 绑到 {@link HarnessToolCallingManager}
 *       （执行结局 append-only 记录）——DanglingCallRepairer 经其按 id 回放（exactly-once）；</li>
 *   <li>会话关闭观察者：run 置 COMPLETED。</li>
 * </ul>
 *
 * <p>用法：{@code Buzhou.runtime(model, stores, RecoverySupport.attach(baseConfig, registry, log, appId), tools)}。
 */
public final class RecoverySupport {

    private RecoverySupport() {
    }

    /** 挂接恢复三件套（ownerId 缺省随机）。 */
    public static RuntimeConfig attach(RuntimeConfig base, RunRegistry registry,
                                       ToolCallLog toolCallLog, String appId) {
        return attach(base, registry, toolCallLog, appId, UUID.randomUUID().toString());
    }

    /** 挂接恢复三件套（指定 ownerId，多实例区分持有者）。 */
    public static RuntimeConfig attach(RuntimeConfig base, RunRegistry registry,
                                       ToolCallLog toolCallLog, String appId, String ownerId) {
        List<BuzhouHook> hooks = new ArrayList<>(base.hooks());
        hooks.add(new RunStateTrackerHook(registry, appId, ownerId));
        List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer> customizers =
                new ArrayList<>(base.assemblyCustomizers());
        customizers.add(bindToolCallLog(toolCallLog, registry));
        return new RuntimeConfig(hooks, base.disabledHookNames(), base.idempotentToolNames(),
                base.viewProcessor(), base.autoTools(), base.serialGroups(),
                base.sessionCustomizers(), customizers, base.turnLoopPolicy());
    }

    private static SessionAssemblyCustomizer bindToolCallLog(ToolCallLog toolCallLog,
                                                             RunRegistry registry) {
        return new SessionAssemblyCustomizer() {
            @Override
            public void customize(SessionAssemblyContext ctx) {
                HarnessToolCallingManager manager = ctx.toolManager();
                if (manager != null) {
                    manager.setToolCallLog(toolCallLog);
                }
                // 会话正常谢幕 → run 置 COMPLETED（崩溃场景无此回调，保持 RUNNING 供恢复枚举）
                ctx.addObserver(new SessionObserver() {
                    @Override
                    public void onClose() {
                        registry.find(ctx.sessionId()).ifPresent(snapshot ->
                                registry.save(snapshot.withStatus(RunStatus.COMPLETED)));
                    }
                });
            }
        };
    }
}
