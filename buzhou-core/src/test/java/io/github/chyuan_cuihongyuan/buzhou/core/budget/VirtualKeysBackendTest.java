package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 315 / impl-338：虚拟 key 共享后端委托回归——计数走后端 / fail-closed /
 * usage 读后端 / reset 透传 / 未注册直通 / 无后端既有行为零变化。
 */
class VirtualKeysBackendTest {

    /** 内存 fake 后端（记录调用 + 可注入故障）。 */
    static final class FakeBackend implements VirtualKeyBudgetBackend {
        final Map<String, AtomicLong> spent = new ConcurrentHashMap<>();
        boolean broken;

        @Override
        public boolean trySpend(String key, long tokens, long limitTokens) {
            if (broken) {
                throw new IllegalStateException("backend down");
            }
            AtomicLong counter = spent.computeIfAbsent(key, k -> new AtomicLong());
            long prev = counter.get();
            while (true) {
                if (prev + tokens > limitTokens) {
                    return false;
                }
                if (counter.compareAndSet(prev, prev + tokens)) {
                    return true;
                }
                prev = counter.get();
            }
        }

        @Override
        public long usedTokens(String key) {
            if (broken) {
                throw new IllegalStateException("backend down");
            }
            AtomicLong counter = spent.get(key);
            return counter == null ? 0L : counter.get();
        }

        @Override
        public void reset(String key) {
            spent.remove(key);
        }
    }

    @Test
    void spendsViaBackendAndUsageReadsBack() {
        FakeBackend backend = new FakeBackend();
        VirtualKeys keys = VirtualKeys.withBackend(backend);
        keys.register("k1", 100L);

        assertThat(keys.trySpend("k1", 60L)).isTrue();
        assertThat(keys.trySpend("k1", 60L)).isFalse(); // 120 > 100
        assertThat(keys.usage("k1").usedTokens()).isEqualTo(60L);
        assertThat(keys.isExhausted("k1")).isTrue(); // 越限拒绝 → 耗尽态
        assertThat(keys.topUsage(10)).extracting(VirtualKeys.KeyUsage::usedTokens)
                .containsExactly(60L);
    }

    @Test
    void backendFailureFailsClosed() {
        FakeBackend backend = new FakeBackend();
        VirtualKeys keys = VirtualKeys.withBackend(backend);
        keys.register("k1", 100L);
        backend.broken = true;

        assertThat(keys.trySpend("k1", 1L))
                .as("后端不可达：宁可拒绝不可超支").isFalse();
        assertThat(keys.isExhausted("k1")).isTrue();
    }

    @Test
    void resetPassesThroughAndClearsExhausted() {
        FakeBackend backend = new FakeBackend();
        VirtualKeys keys = VirtualKeys.withBackend(backend);
        keys.register("k1", 100L);
        keys.trySpend("k1", 100L);
        assertThat(keys.trySpend("k1", 1L)).isFalse();

        keys.reset("k1");

        assertThat(backend.usedTokens("k1")).isZero();
        assertThat(keys.isExhausted("k1")).isFalse();
        assertThat(keys.trySpend("k1", 100L)).isTrue();
    }

    @Test
    void unregisteredKeyPassesThrough() {
        VirtualKeys keys = VirtualKeys.withBackend(new FakeBackend());
        assertThat(keys.trySpend("ghost", 999L)).isTrue();
    }

    @Test
    void noBackendKeepsInProcessBehavior() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("k1", 100L);
        assertThat(keys.trySpend("k1", 100L)).isTrue();
        assertThat(keys.trySpend("k1", 1L)).isFalse();
        assertThat(keys.usage("k1").usedTokens()).isEqualTo(100L);
    }

    @Test
    void factoryRejectsNullBackend() {
        assertThatThrownBy(() -> VirtualKeys.withBackend(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
