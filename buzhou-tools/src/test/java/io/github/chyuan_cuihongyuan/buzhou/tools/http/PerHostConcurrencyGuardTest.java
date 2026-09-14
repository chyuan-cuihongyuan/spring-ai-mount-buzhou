package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * per-host 并发上限测试（spec 1603 / T2357–T2358 / impl 1156）：闸本体（关=无限 /
 * 上限拒绝 / 释放再进 / 跨 host 独立）+ 工具集成（占满后 call() 快速拒绝、
 * stats 第七桶守恒）。Nginx limit_conn 思想。
 */
class PerHostConcurrencyGuardTest {

    @AfterEach
    void reset() {
        HttpRequestTool.resetForTest();
    }

    @Test
    void disabledGuardAdmitsEverything() {
        PerHostConcurrencyGuard guard = new PerHostConcurrencyGuard(0);
        for (int i = 0; i < 100; i++) {
            assertThat(guard.tryEnter("a.example")).isTrue();
        }
        assertThat(guard.inFlight("a.example")).isZero();
        guard.exit("a.example"); // 关闭态 exit 无副作用
    }

    @Test
    void limitRejectsBeyondMaxAndAdmitsAfterRelease() {
        PerHostConcurrencyGuard guard = new PerHostConcurrencyGuard(2);
        assertThat(guard.tryEnter("a.example")).isTrue();
        assertThat(guard.tryEnter("a.example")).isTrue();
        assertThat(guard.tryEnter("a.example")).isFalse();
        assertThat(guard.inFlight("a.example")).isEqualTo(2);
        guard.exit("a.example");
        assertThat(guard.tryEnter("a.example")).isTrue();
        assertThat(guard.inFlight("a.example")).isEqualTo(2);
    }

    @Test
    void hostsAreIndependent() {
        PerHostConcurrencyGuard guard = new PerHostConcurrencyGuard(1);
        assertThat(guard.tryEnter("a.example")).isTrue();
        assertThat(guard.tryEnter("b.example")).isTrue();
        assertThat(guard.tryEnter("a.example")).isFalse();
        assertThat(guard.tryEnter("b.example")).isFalse();
    }

    @Test
    void toolRejectsFastWhenHostFullAndCountsConservation() {
        PerHostConcurrencyGuard guard = new PerHostConcurrencyGuard(1);
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, java.util.List.of()), Duration.ofSeconds(2), guard);
        assertThat(guard.tryEnter("example.com")).isTrue(); // 占满唯一名额
        String out = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/x\"}");
        assertThat(out).contains("并发已达上限").contains("example.com");
        HttpRequestTool.HttpToolStats stats = HttpRequestTool.stats();
        assertThat(stats.hostLimitRejects()).isEqualTo(1);
        // 守恒：attempts = successes + 七桶之和
        assertThat(stats.attempts()).isEqualTo(stats.successes() + stats.totalRejects());
        // 释放名额后同一请求不再被 host 闸拒绝（走到真实网络层——以失败桶/成功桶收尾均可，
        // 断言不含 limit_conn 文案即证明闸已放行）
        guard.exit("example.com");
        String out2 = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/x\"}");
        assertThat(out2).doesNotContain("并发已达上限");
    }

    @Test
    void nullGuardKeepsExistingZeroChangeBehavior() {
        HttpRequestTool tool = new HttpRequestTool(new SsrfGuard(true, java.util.List.of()),
                Duration.ofSeconds(2));
        // 无闸路径：非法方法照旧走 method 拒绝桶（未被 host 闸短路）
        String out = tool.call("{\"method\":\"BREW\",\"url\":\"https://example.com/x\"}");
        assertThat(out).contains("不支持的 method");
        assertThat(HttpRequestTool.stats().methodRejects()).isEqualTo(1);
    }
}
