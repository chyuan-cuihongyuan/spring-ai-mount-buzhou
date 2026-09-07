package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LaneStateBackend;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 316 / impl-339：后端泳道许可回归——acquire/release 委托 / 满道超时
 * fail-closed / 后端异常按满道 / callback 共享路径回喂与释放 / Semaphore 路径零变化。
 */
class BackendLanePermitTest {

    /** 可控 fake 后端（容量 1 + 故障注入）。 */
    static final class FakeLaneBackend implements LaneStateBackend {
        int held;
        boolean broken;

        @Override
        public boolean tryAcquire(String lane, int permits) {
            if (broken) {
                throw new IllegalStateException("backend down");
            }
            if (held + 1 <= permits) {
                held++;
                return true;
            }
            return false;
        }

        @Override
        public void release(String lane) {
            held = Math.max(0, held - 1);
        }

        @Override
        public void reset(String lane) {
            held = 0;
        }
    }

    private static ToolCallback echoTool(AtomicInteger invocations) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return invocations.incrementAndGet() == 1 ? "ok" : "ok2";
            }
        };
    }

    @Test
    void acquireReleaseDelegate() throws InterruptedException {
        FakeLaneBackend backend = new FakeLaneBackend();
        BackendLanePermit permit = BackendLanePermit.of(backend, "slow-db", 2);

        assertThat(permit.tryAcquire(Duration.ofMillis(50))).isTrue();
        assertThat(backend.held).isEqualTo(1);
        permit.release();
        assertThat(backend.held).isZero();
    }

    @Test
    void fullLaneTimesOutFailClosed() throws InterruptedException {
        FakeLaneBackend backend = new FakeLaneBackend();
        backend.held = 2; // slow-db:2 已满
        BackendLanePermit permit = BackendLanePermit.of(backend, "slow-db", 2);

        long start = System.nanoTime();
        assertThat(permit.tryAcquire(Duration.ofMillis(250))).isFalse();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(200L); // 至少轮询一步
    }

    @Test
    void backendFailureTreatedAsFull() throws InterruptedException {
        FakeLaneBackend backend = new FakeLaneBackend();
        backend.broken = true;
        BackendLanePermit permit = BackendLanePermit.of(backend, "slow-db", 2);

        assertThat(permit.tryAcquire(Duration.ofMillis(50)))
                .as("后端不可达按满道（fail-closed 不打穿）").isFalse();
    }

    @Test
    void sharedLaneCallbackExecutesAndReleases() {
        FakeLaneBackend backend = new FakeLaneBackend();
        AtomicInteger invocations = new AtomicInteger();
        LaneLimitingToolCallback callback = LaneLimitingToolCallback.wrap(
                echoTool(invocations), BackendLanePermit.of(backend, "slow-db", 1),
                Duration.ofMillis(200));

        assertThat(callback.call("{}")).isEqualTo("ok");
        assertThat(backend.held).as("用后即还").isZero();

        // 满道时超时词汇与 Semaphore 路径一致
        backend.held = 1;
        assertThatThrownBy(() -> callback.call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("泳道许可等待超时");
    }
}
