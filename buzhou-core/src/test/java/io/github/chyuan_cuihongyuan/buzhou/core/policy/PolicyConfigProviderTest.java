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
}
