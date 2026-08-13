package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;

import java.util.List;
import java.util.Optional;

/**
 * Run 恢复服务（wayfinder2 impl-06 / T32 / docs/spec/12）：重启后枚举在途 run 并安全续跑。
 *
 * <p><b>lease 门</b>：restart 经 {@code spawn}（默认不 steal）——租约仍被他方持有时
 * 抛 {@link SessionAlreadyActiveException}（拿不到即拒绝），补上 Mastra 未明的并发防护。
 * 续跑语义 = 从最后 Completed-Turn 之后（BuzhouChatMemory 加载历史时经
 * DanglingCallRepairer 修复悬空调用；事件日志命中 COMPLETED 的调用按 id 回放、不重跑）。
 */
public class RunRecoveryService {

    private final RunRegistry registry;
    private final AgentRuntime runtime;

    public RunRecoveryService(RunRegistry registry, AgentRuntime runtime) {
        this.registry = registry;
        this.runtime = runtime;
    }

    /** 在途 run（正常运行与疑似崩溃者；配合租约判断归属）。 */
    public List<RunStateSnapshot> runningRuns() {
        return registry.list(RunStatus.RUNNING);
    }

    /**
     * 重启指定 run（重新 spawn 同 sessionId；历史加载触发悬空修复 + 事件日志回放）。
     *
     * <p>租约被他方持有时 spawn 抛 {@link SessionAlreadyActiveException}（lease 门，拿不到即拒绝）；
     * run 不存在或已 COMPLETED 时返回 {@link Optional#empty()}。
     *
     * @param steal 租约仍被他方持有时是否强夺（默认 false = 拿不到即拒绝）
     */
    public Optional<AgentSession> restart(String sessionId, boolean steal) {
        return registry.find(sessionId)
                .filter(snapshot -> snapshot.status() != RunStatus.COMPLETED)
                .map(snapshot -> {
                    registry.save(snapshot.withStatus(RunStatus.INTERRUPTED));
                    SpawnOptions options = steal ? SpawnOptions.withSteal() : SpawnOptions.defaults();
                    return runtime.spawn(snapshot.appId(), snapshot.agentName(),
                            snapshot.sessionId(), options);
                });
    }
}
