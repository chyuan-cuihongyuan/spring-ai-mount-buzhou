package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CancellationToken;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-28 / spec 13 §cross-43：五类故障构件的确定性行为——延迟、失败率（种子可复现）、
 * 永久挂起（吞中断）、资源泄漏计数、中途取消（协作令牌）。
 */
class FaultInjectingToolCallbackTest {

    @Test
    void healthySpecReturnsImmediatelyWithInvocationCount() {
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.none());

        assertThat(tool.call("{}")).isEqualTo(FaultInjectingToolCallback.DEFAULT_TOOL_NAME + " ok #1");
        assertThat(tool.call("{}")).contains("ok #2");
        assertThat(tool.invocations()).isEqualTo(2);
        assertThat(tool.leakedStreams()).isZero();
    }

    @Test
    void delaySpecDefersCompletion() {
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.delayOf(150L));

        long start = System.nanoTime();
        String result = tool.call("{}");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).contains("ok #1");
        assertThat(elapsedMs).isGreaterThanOrEqualTo(100L);
        assertThat(elapsedMs).isLessThan(3_000L);
    }

    @Test
    void failRateIsDeterministicPerSeed() {
        // 同种子 + 同调用序 → 同成败序列（断言可复现）；rate=0 恒成功、rate=1 恒失败
        List<Boolean> firstRun = runFailures(0.5D, 42L, 20);
        List<Boolean> secondRun = runFailures(0.5D, 42L, 20);
        assertThat(firstRun).isEqualTo(secondRun);
        assertThat(firstRun).contains(true).contains(false);

        FaultInjectingToolCallback neverFails = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.failRate(0D, 7L));
        assertThat(neverFails.call("{}")).contains("ok");

        FaultInjectingToolCallback alwaysFails = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.failRate(1D, 7L));
        assertThatThrownBy(() -> alwaysFails.call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fault-injected failure");
    }

    private static List<Boolean> runFailures(double rate, long seed, int calls) {
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.failRate(rate, seed));
        return java.util.stream.IntStream.range(0, calls)
                .mapToObj(i -> {
                    try {
                        tool.call("{}");
                        return false;
                    } catch (IllegalStateException e) {
                        return true;
                    }
                })
                .toList();
    }

    @Test
    void hangForeverIgnoresInterruption() throws Exception {
        // 挂死样本：中断后仍存活（守护虚拟线程，随进程回收）——Deadline 兜底的对象场景
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.hanging());
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interruptedWithoutExit = new AtomicBoolean(false);

        Thread worker = Thread.ofVirtual().unstarted(() -> {
            started.countDown();
            try {
                tool.call("{}");
            } catch (Throwable t) {
                // 永不返回的样本：正常情况不会到这里
                interruptedWithoutExit.set(true);
            }
        });
        worker.start();
        assertThat(started.await(2L, TimeUnit.SECONDS)).isTrue();

        worker.interrupt();
        Thread.sleep(150L);
        assertThat(worker.isAlive()).isTrue();
        assertThat(interruptedWithoutExit.get()).isFalse();
        assertThat(tool.invocations()).isEqualTo(1);
    }

    @Test
    void leakResourceCountsUnclosedStreams() {
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.leaking());

        assertThat(tool.call("{}")).contains("ok #1");
        assertThat(tool.call("{}")).contains("ok #2");
        assertThat(tool.leakedStreams()).isEqualTo(2L);
    }

    @Test
    void cancelMidFlightAbortsWhenTokenSet() {
        // 协作式取消样本：延迟 5s 但令牌预先置位 → 一个轮询片内即抛「cancelled mid-flight」
        FaultInjectingToolCallback tool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.cancelAware(5_000L));
        AtomicBoolean cancelled = new AtomicBoolean(true);
        ToolContext context = new ToolContext(Map.of(
                CancellationToken.KEY, CancellationToken.of(cancelled::get)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> tool.call("{}", context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled mid-flight");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(3_000L);

        // 令牌未置位：延迟正常走完
        AtomicBoolean idle = new AtomicBoolean(false);
        ToolContext quiet = new ToolContext(Map.of(
                CancellationToken.KEY, CancellationToken.of(idle::get)));
        FaultInjectingToolCallback shortDelay = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.cancelAware(30L));
        assertThat(shortDelay.call("{}", quiet)).contains("ok");
    }

    @Test
    void specValidatesIllegalArguments() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        FaultInjectingToolCallback.FaultSpec.delayOf(-1L))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        FaultInjectingToolCallback.FaultSpec.failRate(1.5D, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
