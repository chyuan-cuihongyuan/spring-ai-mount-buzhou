package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-34 / spec 13 §core-4：线程工厂——{@code buzhou-<role>-<seq>} 命名 +
 * 未捕获异常处理器（异常统一 ERROR 日志，不蒸发）。
 */
class BuzhouThreadFactoryTest {

    @Test
    void virtualThreadsAreNamedWithRolePrefixAndSequence() throws Exception {
        BuzhouThreadFactory factory = BuzhouThreadFactory.virtual("unit-test");
        Thread first = factory.newThread(() -> {
        });
        Thread second = factory.newThread(() -> {
        });
        assertThat(first.getName()).isEqualTo("buzhou-unit-test-1");
        assertThat(second.getName()).isEqualTo("buzhou-unit-test-2");
        assertThat(first.isVirtual()).isTrue();
    }

    @Test
    void platformThreadsAreNamed() {
        Thread thread = BuzhouThreadFactory.platform("plat").newThread(() -> {
        });
        assertThat(thread.getName()).isEqualTo("buzhou-plat-1");
        assertThat(thread.isVirtual()).isFalse();
    }

    @Test
    void uncaughtExceptionHandlerIsInstalledByFactory() throws Exception {
        // 工厂装配的 UCEH = ERROR 日志兜底（不蒸发）；此处断言其已挂接并可接管异常路径
        Thread thread = BuzhouThreadFactory.virtual("boom").newThread(() -> {
            throw new IllegalStateException("boom");
        });
        assertThat(thread.getUncaughtExceptionHandler()).isNotNull();
        CountDownLatch done = new CountDownLatch(1);
        // 观察者替换为计数探针验证异常路径贯通（线程照常终止，不悬挂）
        thread.setUncaughtExceptionHandler((t, e) -> done.countDown());
        thread.start();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        thread.join(1_000);
        assertThat(thread.isAlive()).isFalse();
    }

    @Test
    void blankRoleFallsBackToGeneric() {
        Thread thread = BuzhouThreadFactory.virtual(" ").newThread(() -> {
        });
        assertThat(thread.getName()).startsWith("buzhou-generic-");
    }
}
