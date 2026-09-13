package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionResourceRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.model.tool.DefaultToolCallingManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RecoverySupport 装配面直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>断言 attach 的三件套挂接：run-state-tracker hook 追加、清理贡献者
 * （run-registry / tool-call-log）回调触达 fake、customizer 绑定 ToolCallLog 到
 * manager 并注册 onClose→COMPLETED 观察者、toolManager 缺席的 null 防御。
 */
class RecoverySupportTest {

    private static final String SESSION = "s-recovery";
    private static final String APP_ID = "app-1";
    private static final String OWNER_ID = "owner-1";

    /** Map 版 RunRegistry fake：记录 deleteSession 调用。 */
    private static final class MapRunRegistry implements RunRegistry {
        private final Map<String, RunStateSnapshot> snapshots = new HashMap<>();
        private final List<String> deletedSessions = new ArrayList<>();

        @Override
        public void save(RunStateSnapshot snapshot) {
            snapshots.put(snapshot.sessionId(), snapshot);
        }

        @Override
        public Optional<RunStateSnapshot> find(String sessionId) {
            return Optional.ofNullable(snapshots.get(sessionId));
        }

        @Override
        public List<RunStateSnapshot> list(RunStatus status) {
            return snapshots.values().stream().filter(s -> s.status() == status).toList();
        }

        @Override
        public void deleteSession(String sessionId) {
            deletedSessions.add(sessionId);
            snapshots.remove(sessionId);
        }
    }

    /** 记录型 ToolCallLog fake。 */
    private static final class RecordingToolCallLog implements ToolCallLog {
        private final List<ToolCallLogEntry> entries = new ArrayList<>();
        private final List<String> deletedSessions = new ArrayList<>();

        @Override
        public void append(ToolCallLogEntry entry) {
            entries.add(entry);
        }

        @Override
        public Optional<ToolCallLogEntry> find(String sessionId, String toolCallId) {
            return Optional.empty();
        }

        @Override
        public void deleteSession(String sessionId) {
            deletedSessions.add(sessionId);
        }
    }

    /** SessionAssemblyContext stub：记录 observer；toolManager 可缺席。 */
    private static final class StubAssemblyContext implements SessionAssemblyContext {
        private final List<SessionObserver> observers = new ArrayList<>();
        private final HarnessToolCallingManager toolManager;

        private StubAssemblyContext(HarnessToolCallingManager toolManager) {
            this.toolManager = toolManager;
        }

        @Override
        public String appId() {
            return APP_ID;
        }

        @Override
        public String agentName() {
            return "agent-a";
        }

        @Override
        public String sessionId() {
            return SESSION;
        }

        @Override
        public BuzhouStores stores() {
            return null;
        }

        @Override
        public SessionResourceRegistry registry() {
            return null;
        }

        @Override
        public SpanContextCarrier spanContextCarrier() {
            return null;
        }

        @Override
        public List<Advisor> advisors() {
            return List.of();
        }

        @Override
        public void addAdvisor(Advisor advisor) {
            // 测试不关心
        }

        @Override
        public void wrapToolCallbacks(UnaryOperator<org.springframework.ai.tool.ToolCallback> wrapper) {
            // 测试不关心
        }

        @Override
        public void addToolCallbacks(List<org.springframework.ai.tool.ToolCallback> tools) {
            // 测试不关心
        }

        @Override
        public void addObserver(SessionObserver observer) {
            observers.add(observer);
        }

        @Override
        public HarnessToolCallingManager toolManager() {
            return toolManager;
        }
    }

    private static HarnessToolCallingManager cheapManager() {
        return new HarnessToolCallingManager(DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(5), Map.of());
    }

    @Test
    void attachAppendsRunStateTrackerHookAndPreservesBaseHooks() {
        MapRunRegistry registry = new MapRunRegistry();
        BuzhouHook baseHook = new BuzhouHook() {
            @Override
            public String name() {
                return "base-hook";
            }
        };
        RuntimeConfig base = RuntimeConfig.hooks(List.of(baseHook));

        RuntimeConfig attached = RecoverySupport.attach(base, registry,
                new RecordingToolCallLog(), APP_ID, OWNER_ID);

        assertThat(attached.hooks()).hasSize(2);
        assertThat(attached.hooks().getFirst()).isSameAs(baseHook);
        assertThat(attached.hooks().getLast().name()).isEqualTo("run-state-tracker");
    }

    @Test
    void attachWiresCleanupContributorsThatReachFakes() {
        MapRunRegistry registry = new MapRunRegistry();
        RecordingToolCallLog log = new RecordingToolCallLog();

        RuntimeConfig attached = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, APP_ID, OWNER_ID);

        Map<String, SessionCleanupContributor> byName = new HashMap<>();
        attached.sessionCleanupContributors().forEach(c -> byName.put(c.name(), c));
        assertThat(byName).containsKeys("run-registry", "tool-call-log");

        byName.get("run-registry").deletion().accept(SESSION);
        byName.get("tool-call-log").deletion().accept(SESSION);
        assertThat(registry.deletedSessions).containsExactly(SESSION);
        assertThat(log.deletedSessions).containsExactly(SESSION);
    }

    @Test
    void customizerBindsToolCallLogAndCloseObserverCompletesRun() {
        MapRunRegistry registry = new MapRunRegistry();
        RecordingToolCallLog log = new RecordingToolCallLog();
        registry.save(new RunStateSnapshot(SESSION, APP_ID, "agent-a", RunStatus.RUNNING,
                1, 0, OWNER_ID, null));

        RuntimeConfig attached = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, APP_ID, OWNER_ID);
        HarnessToolCallingManager manager = cheapManager();
        StubAssemblyContext ctx = new StubAssemblyContext(manager);

        attached.assemblyCustomizers().getLast().customize(ctx);
        assertThat(manager.toolCallLog()).isSameAs(log);
        assertThat(ctx.observers).hasSize(1);

        // 会话谢幕 → run 置 COMPLETED（崩溃场景无此回调，保持 RUNNING 供恢复枚举）
        ctx.observers.getFirst().onClose();
        assertThat(registry.find(SESSION).orElseThrow().status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(registry.list(RunStatus.RUNNING)).isEmpty();
    }

    @Test
    void customizerToleratesMissingToolManager() {
        MapRunRegistry registry = new MapRunRegistry();
        RecordingToolCallLog log = new RecordingToolCallLog();

        RuntimeConfig attached = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, APP_ID, OWNER_ID);
        StubAssemblyContext ctx = new StubAssemblyContext(null);

        assertThatCode(() -> attached.assemblyCustomizers().getLast().customize(ctx))
                .doesNotThrowAnyException();
        // manager 缺席不阻断 observer 注册
        assertThat(ctx.observers).hasSize(1);
    }

    @Test
    void onCloseWithoutSnapshotIsNoop() {
        MapRunRegistry registry = new MapRunRegistry();
        RecordingToolCallLog log = new RecordingToolCallLog();

        RuntimeConfig attached = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, APP_ID, OWNER_ID);
        StubAssemblyContext ctx = new StubAssemblyContext(null);
        attached.assemblyCustomizers().getLast().customize(ctx);

        assertThatCode(() -> ctx.observers.getFirst().onClose()).doesNotThrowAnyException();
        assertThat(registry.find(SESSION)).isEmpty();
    }
}
