package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1430 / T2162：会话关闭耗时读数——正常 close 计数/耗时/水位、
 * 幂等 close 不重复计、observer 失败分桶且清理继续（既有语义）、reset。
 * 静态面测试前后归零防串扰。
 */
class SessionCloseStatsTest {

    @BeforeEach
    void reset() {
        SessionCloseStats.resetForTest();
    }

    @AfterEach
    void resetAfter() {
        SessionCloseStats.resetForTest();
    }

    private AgentSession session() {
        ScriptedChatModel model = new ScriptedChatModel();
        return Buzhou.runtime(model, Buzhou.inMemoryStores(),
                new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                        null, List.of()))
                .spawn("app", "ag", "s-close");
    }

    @Test
    void normalCloseFeedsCounters() {
        AgentSession session = session();
        session.close();
        SessionCloseStats.Snapshot s = SessionCloseStats.stats();
        assertThat(s.closed()).isEqualTo(1);
        assertThat(s.closeFailures()).isZero();
        assertThat(s.lastCloseDurationMillis()).isGreaterThanOrEqualTo(0);
        assertThat(s.maxCloseDurationMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void idempotentCloseDoesNotDoubleCount() {
        AgentSession session = session();
        session.close();
        session.close(); // 幂等——CAS false 直接返回
        assertThat(SessionCloseStats.stats().closed()).isEqualTo(1);
    }

    @Test
    void observerFailureBucketsButCloseStillCounts() {
        ScriptedChatModel model = new ScriptedChatModel();
        var config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), java.util.Map.of(), List.of(),
                List.of(ctx -> ctx.addObserver(new SessionObserver() {
                    @Override
                    public void onClose() {
                        throw new IllegalStateException("observer 关闭失败");
                    }
                })),
                null);
        AgentSession session = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-close-fail");
        // close 契约：清理完毕后首失败上抛（throwAggregated）——但埋点已入账
        assertThatThrownBy(session::close).isInstanceOf(IllegalStateException.class);
        assertThat(SessionCloseStats.stats().closeFailures()).isEqualTo(1);
        assertThat(SessionCloseStats.stats().closed()).isEqualTo(1);
    }

    @Test
    void maxWatermarkMonotonicAndReset() {
        AgentSession s1 = session();
        s1.close();
        long first = SessionCloseStats.stats().maxCloseDurationMillis();
        AgentSession s2 = session();
        s2.close();
        assertThat(SessionCloseStats.stats().maxCloseDurationMillis())
                .isGreaterThanOrEqualTo(first);
        SessionCloseStats.resetForTest();
        assertThat(SessionCloseStats.stats()).isEqualTo(
                new SessionCloseStats.Snapshot(0, 0, 0, 0));
    }
}
