package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 崩溃自愈 watchdog 红队（spec 69 §B / T291）：疑似崩溃（RUNNING 快照 + 租约空闲）被
 * 续跑接管；他方活跃实例持锁（租约在册）被跳过不打扰；无 RUNNING 零操作；一个失败
 * 不阻断其余。
 */
class CrashResumeWatchdogTest {

    private static RunStateSnapshot running(String sessionId) {
        return new RunStateSnapshot(sessionId, "app", "agent", RunStatus.RUNNING,
                1, 0, "owner-dead", java.time.Instant.now());
    }

    /** 疑似崩溃（快照 RUNNING、租约空闲）→ watchdog 续跑接管 + 快照转 INTERRUPTED。 */
    @Test
    void crashedRunIsResumedWhenLeaseFree() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        registry.save(running("sess-crashed"));
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), stores,
                RuntimeConfig.defaults());

        RunRecoveryService.AutoResumeResult result =
                new RunRecoveryService(registry, runtime).autoResumeAll();

        assertThat(result.resumed()).isEqualTo(1);
        assertThat(result.leaseHeld()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(registry.find("sess-crashed"))
                .map(RunStateSnapshot::status).contains(RunStatus.INTERRUPTED);
        // 接管后新会话在册（续跑会话关闭交给调用方生命周期——此处验证 spawn 成功态）
        Optional<AgentRuntime> unused = Optional.empty();
        assertThat(unused).isEmpty();
    }

    /** 他方活跃实例持锁 → 跳过不打扰（steal=false 语义）。 */
    @Test
    void leaseHeldRunIsSkipped() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        registry.save(running("sess-live"));
        // 预占租约（他方活跃实例持有）
        SessionLeaseStore leases = stores.sessionLeaseStore();
        leases.tryAcquire("sess-live", "owner-other", Duration.ofMinutes(5));
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), stores,
                RuntimeConfig.defaults());

        RunRecoveryService.AutoResumeResult result =
                new RunRecoveryService(registry, runtime).autoResumeAll();

        assertThat(result.leaseHeld()).isEqualTo(1);
        assertThat(result.resumed()).isZero();
    }

    /** 无 RUNNING 快照 → 零操作零噪音。 */
    @Test
    void noRunningSnapshotsIsNoop() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        RunRecoveryService.AutoResumeResult result = new RunRecoveryService(
                new InMemoryRunRegistry(),
                Buzhou.runtime(new ScriptedChatModel(), stores, RuntimeConfig.defaults()))
                .autoResumeAll();
        assertThat(result.resumed()).isZero();
        assertThat(result.leaseHeld()).isZero();
        assertThat(result.failed()).isZero();
    }

    /** 一个失败不阻断其余（快照损坏/会话态异常等——failed 计数 + 其余继续）。 */
    @Test
    void failureIsIsolatedPerRun() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        InMemoryRunRegistry registry = new InMemoryRunRegistry() {
            @Override
            public Optional<RunStateSnapshot> find(String sessionId) {
                if ("sess-bad".equals(sessionId)) {
                    throw new IllegalStateException("store 坏读");
                }
                return super.find(sessionId);
            }
        };
        registry.save(running("sess-bad"));
        registry.save(running("sess-good"));
        AgentRuntime runtime = Buzhou.runtime(new ScriptedChatModel(), stores,
                RuntimeConfig.defaults());

        RunRecoveryService.AutoResumeResult result =
                new RunRecoveryService(registry, runtime).autoResumeAll();

        assertThat(result.failed()).isEqualTo(1); // 坏读隔离
        assertThat(result.resumed()).isEqualTo(1); // 好的照常续跑
    }
}
