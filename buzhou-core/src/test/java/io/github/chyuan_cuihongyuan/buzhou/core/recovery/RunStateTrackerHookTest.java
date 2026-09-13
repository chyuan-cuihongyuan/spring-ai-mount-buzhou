package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RunStateTrackerHook 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>手写 Map 版 {@link RunRegistry} fake，断言快照新建（orElseGet 分支）、同一快照上
 * 推进（startingTurn/completingTurn 不另建记录）、CONTINUE 结果与 hook 名（按名禁用依赖）。
 */
class RunStateTrackerHookTest {

    private static final String SESSION = "s-run";
    private static final String APP_ID = "app-1";
    private static final String OWNER_ID = "owner-1";
    private static final int TURN = 3;

    /** 定值 TurnContext stub：仅 hook 消费的 sessionId/agentName/turn 有意义。 */
    private static TurnContext turnContext() {
        return new TurnContext() {
            @Override
            public String sessionId() {
                return SESSION;
            }

            @Override
            public String agentName() {
                return "agent-a";
            }

            @Override
            public int turn() {
                return TURN;
            }

            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(SessionEvent event) {
                // 测试不关心事件
            }

            @Override
            public String input() {
                return "";
            }

            @Override
            public String response() {
                return "";
            }

            @Override
            public void replaceInput(String newInput) {
                // 测试不关心
            }

            @Override
            public void replaceResponse(String newResponse) {
                // 测试不关心
            }
        };
    }

    /** Map 版 RunRegistry fake：save upsert、find/list 直读。 */
    private static final class MapRunRegistry implements RunRegistry {
        private final Map<String, RunStateSnapshot> snapshots = new HashMap<>();

        @Override
        public void save(RunStateSnapshot snapshot) {
            snapshots.put(snapshot.sessionId(), snapshot);
        }

        @Override
        public Optional<RunStateSnapshot> find(String sessionId) {
            return Optional.ofNullable(snapshots.get(sessionId));
        }

        @Override
        public java.util.List<RunStateSnapshot> list(RunStatus status) {
            return snapshots.values().stream().filter(s -> s.status() == status).toList();
        }
    }

    @Test
    void beforeTurnCreatesRunningSnapshotWhenNoneExists() {
        MapRunRegistry registry = new MapRunRegistry();
        RunStateTrackerHook hook = new RunStateTrackerHook(registry, APP_ID, OWNER_ID);

        assertThat(hook.name()).isEqualTo("run-state-tracker");
        assertThat(hook.beforeTurn(turnContext())).isSameAs(HookResult.CONTINUE);

        RunStateSnapshot snapshot = registry.find(SESSION).orElseThrow();
        assertThat(snapshot.status()).isEqualTo(RunStatus.RUNNING);
        assertThat(snapshot.sessionId()).isEqualTo(SESSION);
        assertThat(snapshot.appId()).isEqualTo(APP_ID);
        assertThat(snapshot.ownerId()).isEqualTo(OWNER_ID);
        assertThat(snapshot.agentName()).isEqualTo("agent-a");
        assertThat(snapshot.currentTurn()).isEqualTo(TURN);
        assertThat(snapshot.lastCompletedTurn()).isZero();
    }

    @Test
    void turnsAdvanceOnTheSameSnapshotNotNewRecords() {
        MapRunRegistry registry = new MapRunRegistry();
        RunStateTrackerHook hook = new RunStateTrackerHook(registry, APP_ID, OWNER_ID);

        hook.beforeTurn(turnContext());
        hook.afterTurn(turnContext());
        assertThat(registry.find(SESSION).orElseThrow().lastCompletedTurn()).isEqualTo(TURN);
        assertThat(registry.snapshots).hasSize(1);

        // 下一轮：同一快照推进 currentTurn，lastCompletedTurn 保留
        hook.beforeTurn(turnContext());
        RunStateSnapshot snapshot = registry.find(SESSION).orElseThrow();
        assertThat(snapshot.currentTurn()).isEqualTo(TURN);
        assertThat(snapshot.lastCompletedTurn()).isEqualTo(TURN);
        assertThat(registry.snapshots).hasSize(1);
    }

    @Test
    void afterTurnReturnsContinueAndKeepsStatusRunning() {
        MapRunRegistry registry = new MapRunRegistry();
        RunStateTrackerHook hook = new RunStateTrackerHook(registry, APP_ID, OWNER_ID);
        hook.beforeTurn(turnContext());

        assertThat(hook.afterTurn(turnContext())).isSameAs(HookResult.CONTINUE);
        assertThat(registry.find(SESSION).orElseThrow().status()).isEqualTo(RunStatus.RUNNING);
    }

    @Test
    void supplierConstructorDefersAppIdAndOwnerIdResolution() {
        MapRunRegistry registry = new MapRunRegistry();
        RunStateTrackerHook hook = new RunStateTrackerHook(registry,
                () -> APP_ID + "-deferred", () -> OWNER_ID + "-deferred");
        hook.beforeTurn(turnContext());

        RunStateSnapshot snapshot = registry.find(SESSION).orElseThrow();
        assertThat(snapshot.appId()).isEqualTo(APP_ID + "-deferred");
        assertThat(snapshot.ownerId()).isEqualTo(OWNER_ID + "-deferred");
    }
}
