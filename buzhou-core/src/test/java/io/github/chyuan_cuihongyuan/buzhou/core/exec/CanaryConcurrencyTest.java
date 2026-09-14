package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金丝雀工具并发语义测试（spec 1606 / T2363–T2364 / impl 1159）：工具执行在
 * monitor 外——两臂同时执行不互相阻塞（锁内执行时代码会串行化：第二个调用必须
 * 等第一个的工具时长），计数与回滚仍原子/最终一致。Netty「不阻塞事件循环」铁律
 * + JDK 虚拟线程 pinning 同源。
 */
class CanaryConcurrencyTest {

    /** 到点后一起放行的阻塞工具（进入即记数 + latch 倒数）。 */
    static final class GatedTool implements ToolCallback {
        final CountDownLatch bothEntered;
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger entered = new AtomicInteger();

        GatedTool(CountDownLatch bothEntered) {
            this.bothEntered = bothEntered;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            entered.incrementAndGet();
            bothEntered.countDown();
            try {
                // 两臂都已进入才放行——若执行仍在 monitor 内（串行化），
                // 第二臂永远进不来，await 超时导致断言失败
                assertThat(release.await(2, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        }
    }

    @Test
    void concurrentStableAndCanaryExecuteInParallelOutsideMonitor() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        GatedTool stable = new GatedTool(bothEntered);
        GatedTool canary = new GatedTool(bothEntered);
        // weight=100：canary 必中；weight=0 分流不可控——用两个 100% 单臂实例并行驱动
        CanaryToolCallback canaryOnly = CanaryToolCallback.wrap(
                nameMismatchSafe(stable), canary, 100, 5, 10, () -> 0.0);
        CanaryToolCallback stableOnly = CanaryToolCallback.wrap(
                stable, nameMismatchSafe(canary), 0, 5, 10, () -> 0.99);

        Thread t1 = new Thread(() -> canaryOnly.call("{}"));
        Thread t2 = new Thread(() -> stableOnly.call("{}"));
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // 两臂都进入阻塞段 = 执行确在锁外并行（锁内执行则会互相等待直至超时）
        assertThat(bothEntered.await(2, TimeUnit.SECONDS))
                .as("stable 与 canary 执行应并行（monitor 外）").isTrue();
        stable.release.countDown();
        canary.release.countDown();
        t1.join(2000);
        t2.join(2000);

        // 计数原子性：各臂恰好一次
        assertThat(stable.entered.get()).isEqualTo(1);
        assertThat(canary.entered.get()).isEqualTo(1);
        CanaryToolCallback.View v = stableOnly.view();
        assertThat(v.stableCalls()).isEqualTo(1);
        assertThat(canaryOnly.view().canaryCalls()).isEqualTo(1);
    }

    /** 同名占位（wrap 校验要求两臂同名——对侧用不参与分流的安全实现）。 */
    private static ToolCallback nameMismatchSafe(GatedTool shape) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("不应被调用（权重 0/100 之外的臂）");
            }
        };
    }
}
