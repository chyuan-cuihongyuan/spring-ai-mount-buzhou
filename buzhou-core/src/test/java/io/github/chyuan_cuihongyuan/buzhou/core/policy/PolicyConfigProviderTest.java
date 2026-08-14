package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.DbPolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.PropertiesPolicyConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyConfigProviderTest {

    private BindingPolicy policy(String appId, long version) {
        return new BindingPolicy(appId, "ops-agent",
                Map.of("spill", Map.of("spill-threshold-chars", 16000)),
                List.of("log-triage"), List.of(), version);
    }

    @Test
    void propertiesProviderServesStaticSnapshot() {
        PropertiesPolicyConfigProvider provider = new PropertiesPolicyConfigProvider(
                Map.of("app1:ops-agent", policy("app1", 1)));

        BindingPolicy found = provider.getBindingPolicy("app1", "ops-agent");
        assertThat(found.skillNames()).containsExactly("log-triage");
        assertThat(provider.getBindingPolicy("nobody", "ops-agent").version()).isZero();
    }

    @Test
    void dbProviderReadsPersistedPolicyAcrossInstances() {
        InMemoryBindingPolicyStore store = new InMemoryBindingPolicyStore();
        store.save(policy("app1", 1));

        DbPolicyConfigProvider first = new DbPolicyConfigProvider(store, Duration.ofDays(1));
        DbPolicyConfigProvider afterRestart = new DbPolicyConfigProvider(store, Duration.ofDays(1));

        assertThat(afterRestart.getBindingPolicy("app1", "ops-agent").mechanismOverrides())
                .isEqualTo(first.getBindingPolicy("app1", "ops-agent").mechanismOverrides());
        first.close();
        afterRestart.close();
    }

    @Test
    void dbProviderNotifiesListenersOnVersionChange() throws InterruptedException {
        InMemoryBindingPolicyStore store = new InMemoryBindingPolicyStore();
        store.save(policy("app1", 1));
        DbPolicyConfigProvider provider = new DbPolicyConfigProvider(store, Duration.ofMillis(20));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BindingPolicy> changed = new AtomicReference<>();
        provider.addChangeListener(p -> {
            changed.set(p);
            latch.countDown();
        });
        provider.startWatching("app1", "ops-agent");

        store.save(policy("app1", 2));
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(changed.get().version()).isEqualTo(2);
        provider.close();
    }

    /**
     * impl-34 / spec 13 §core-4：轮询失败指数退避——武装故障后首轮轮询（~300ms）失败，
     * 退避 600ms（下一轮 ≥900ms）：t=700ms 时仍只有 2 次 find（startWatching 内联 1 次 +
     * 失败轮询 1 次；固定节奏下 ~600ms 处已有第 3 次）。恢复后节奏回归 300ms。
     */
    @Test
    void dbProviderBacksOffExponentiallyAfterPollFailure() throws InterruptedException {
        AtomicInteger polls = new AtomicInteger();
        AtomicBoolean armed = new AtomicBoolean(); // startWatching 完成后才武装（内联首查不炸）
        BindingPolicyStore flaky = new BindingPolicyStore() {
            @Override
            public java.util.Optional<BindingPolicy> find(String appId, String agentName) {
                polls.incrementAndGet();
                if (armed.compareAndSet(true, false)) {
                    throw new IllegalStateException("模拟存储瞬断");
                }
                return java.util.Optional.of(policy(appId, 42));
            }

            @Override
            public void save(BindingPolicy policy) {
            }
        };
        DbPolicyConfigProvider provider = new DbPolicyConfigProvider(flaky, Duration.ofMillis(300));
        provider.startWatching("app1", "ops-agent");
        armed.set(true);

        // t≈300ms：失败轮询（polls=2）；退避 600ms → 下一轮 ≥900ms。t=700ms 处仍为 2 次
        Thread.sleep(700);
        assertThat(polls.get()).isEqualTo(2);
        // 恢复轮（~900ms）成功；节奏回归 300ms——3s 内累计 ≥5 次（内联1 + 失败1 + 恢复3）
        long deadline = System.currentTimeMillis() + 3_000;
        while (polls.get() < 5 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(polls.get()).isGreaterThanOrEqualTo(5);
        provider.close();
    }
}
