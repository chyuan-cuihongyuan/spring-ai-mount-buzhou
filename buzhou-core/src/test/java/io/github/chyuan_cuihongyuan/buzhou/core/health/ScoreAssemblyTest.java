package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-670 / spec 917：健康评分端点装配——快照含 score 段（数值/tier/清单）、
 * mechanisms 段原样共存、compute 异常降级不炸端点、既有零回归。
 */
class ScoreAssemblyTest {

    private static BuzhouHealth mech(String name, BuzhouHealth.Status status) {
        return new BuzhouHealth() {
            @Override
            public String mechanism() {
                return name;
            }

            @Override
            public Status status() {
                return status;
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    void snapshotContainsScoreSegment() {
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(List.of(
                mech("a", BuzhouHealth.Status.UP),
                mech("broken", BuzhouHealth.Status.DOWN)));
        Map<String, Object> snapshot = endpoint.buzhouSnapshot();

        assertThat(snapshot).containsKey("mechanisms"); // 既有段共存
        assertThat(snapshot).containsKey("score");
        Map<String, Object> score = (Map<String, Object>) snapshot.get("score");
        assertThat(score.get("score")).isEqualTo(50); // (100+0)/2
        assertThat(score.get("tier")).isEqualTo("unhealthy");
        assertThat(score.get("downCount")).isEqualTo(1);
        assertThat((List<String>) score.get("downMechanisms")).containsExactly("broken");
    }

    @Test
    void allUpScoresHundredInSnapshot() {
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(List.of(
                mech("a", BuzhouHealth.Status.UP),
                mech("b", BuzhouHealth.Status.UP)));
        @SuppressWarnings("unchecked")
        Map<String, Object> score = (Map<String, Object>) endpoint.buzhouSnapshot().get("score");
        assertThat(score.get("score")).isEqualTo(100);
        assertThat(score.get("tier")).isEqualTo("healthy");
    }

    @Test
    void computeFailureDegradesGracefully() {
        BuzhouHealth explosive = new BuzhouHealth() {
            @Override
            public String mechanism() {
                throw new IllegalStateException("机制名爆炸");
            }

            @Override
            public Status status() {
                throw new IllegalStateException("状态爆炸");
            }
        };
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(List.of(explosive));
        Map<String, Object> snapshot = endpoint.buzhouSnapshot(); // 不抛（三处读取全部隔离）
        @SuppressWarnings("unchecked")
        Map<String, Object> mechanisms = (Map<String, Object>) snapshot.get("mechanisms");
        // mechanism() 爆炸降级占位键（类名兜底），status 降级 DOWN
        assertThat(mechanisms).containsKey("unknown-mechanism@" + explosive.getClass().getName());
        @SuppressWarnings("unchecked")
        Map<String, Object> score = (Map<String, Object>) snapshot.get("score");
        assertThat(score).containsKey("scoreError"); // compute 内部再炸 → safeScore 降级不炸端点
    }
}
