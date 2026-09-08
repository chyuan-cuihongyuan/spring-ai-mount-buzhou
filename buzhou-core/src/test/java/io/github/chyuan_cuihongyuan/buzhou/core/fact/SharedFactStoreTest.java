package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 410 §Testing / T711–T712：共享事实 ACL——owner 恒读+覆盖自己；
 * grant/revoke 幂等；deny-by-default + 计数；键即所有权 fail-fast；
 * ttl 过期；readable 集合；bean 恒在。
 */
class SharedFactStoreTest {

    private static final class SettableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-08T00:00:00Z");

        void plusSeconds(long s) {
            now = now.plusSeconds(s);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private static SharedFact fact(String key, String owner, Duration ttl) {
        return new SharedFact(key, "v-" + key, owner, Instant.parse("2026-09-08T00:00:00Z"), ttl);
    }

    @Test
    void shouldEnforceDenyByDefaultWithGrantRevokeSemantics() {
        InMemorySharedFactStore store = new InMemorySharedFactStore();
        store.publish(fact("user.city", "agent-a", null));

        // owner 恒读；旁路 reader deny-by-default
        assertThat(store.read("agent-a", "user.city")).contains("v-user.city");
        assertThat(store.read("agent-b", "user.city")).isEmpty();
        assertThat(store.deniedReads()).isEqualTo(1);

        // grant → 可读；revoke → 拒绝（幂等）
        store.grant("user.city", "agent-b");
        assertThat(store.read("agent-b", "user.city")).contains("v-user.city");
        store.revoke("user.city", "agent-b");
        store.revoke("user.city", "agent-b"); // 幂等
        assertThat(store.read("agent-b", "user.city")).isEmpty();
        assertThat(store.deniedReads()).isEqualTo(2);

        // owner 覆盖自己的键 OK；revoke owner 无效（恒读）
        store.publish(fact("user.city", "agent-a", null));
        store.revoke("user.city", "agent-a");
        assertThat(store.read("agent-a", "user.city")).isPresent();

        // grant 不存在的键 fail-fast；重复 grant 幂等
        assertThatThrownBy(() -> store.grant("nope", "agent-b"))
                .isInstanceOf(IllegalArgumentException.class);
        store.grant("user.city", "agent-c");
        store.grant("user.city", "agent-c");
        assertThat(store.read("agent-c", "user.city")).isPresent();
    }

    @Test
    void shouldFailFastWhenNonOwnerPublishesExistingKey() {
        InMemorySharedFactStore store = new InMemorySharedFactStore();
        store.publish(fact("owned", "agent-a", null));
        assertThatThrownBy(() -> store.publish(fact("owned", "agent-b", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("键即所有权");
        // 原值未被覆盖
        assertThat(store.read("agent-a", "owned")).contains("v-owned");
    }

    @Test
    void shouldExpireByTtl_andListReadable() {
        SettableClock clock = new SettableClock();
        InMemorySharedFactStore store = new InMemorySharedFactStore(clock);
        store.publish(fact("perm", "agent-a", null));
        store.publish(new SharedFact("temp", "v-temp", "agent-a", clock.instant(),
                Duration.ofSeconds(60)));
        store.grant("perm", "agent-b");

        assertThat(store.readable("agent-b")).hasSize(1); // temp 是 owner 的、未授权
        assertThat(store.readable("agent-a")).hasSize(2);

        clock.plusSeconds(61); // temp 过期
        assertThat(store.read("agent-a", "temp")).isEmpty();
        assertThat(store.readable("agent-a")).hasSize(1);
        assertThat(store.readable("agent-b")).hasSize(1).first()
                .extracting(SharedFact::key).isEqualTo("perm");
    }

    @Test
    void shouldProvideBeanThroughAssembly() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouSharedFactStore");
                });
    }
}
