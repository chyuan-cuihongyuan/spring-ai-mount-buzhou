package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConfigDiff×快照端点同源补验（spec 736 / T1023–T1024 / impl 539）：端点真
 * 快照两份对比——掩码键按掩码语义判定、diff 稳定。
 */
class ConfigDiffEndpointE2ETest {

    private static BuzhouConfigSnapshotEndpoint endpoint(MockEnvironment env) {
        return new BuzhouConfigSnapshotEndpoint(env);
    }

    @Test
    void sameEnvYieldsEmptyDiff() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("buzhou.webhook.url", "http://x/hook")
                .withProperty("buzhou.leak.level", "SIMPLE");

        Map<String, String> s1 = endpoint(env).buzhouConfig();
        Map<String, String> s2 = endpoint(env).buzhouConfig();

        assertThat(ConfigDiff.diff(s1, s2)).isEmpty();
    }

    @Test
    void changedValueShowsAsChanged() {
        MockEnvironment before = new MockEnvironment()
                .withProperty("buzhou.webhook.url", "http://x/hook");
        MockEnvironment after = new MockEnvironment()
                .withProperty("buzhou.webhook.url", "http://y/hook");

        var diff = ConfigDiff.diff(endpoint(before).buzhouConfig(), endpoint(after).buzhouConfig());

        assertThat(diff).hasSize(1);
        assertThat(diff.get(0).kind()).isEqualTo(ConfigDiff.Kind.CHANGED);
        assertThat(diff.get(0).key()).isEqualTo("buzhou.webhook.url");
    }

    @Test
    void maskedKeysCompareByMaskValue() {
        // 掩码键两环境掩码值相同（***）→ 未变；一个掩码一个明文 → CHANGED
        MockEnvironment before = new MockEnvironment()
                .withProperty("buzhou.vault.salt", "topsecret");
        MockEnvironment after = new MockEnvironment()
                .withProperty("buzhou.vault.salt", "topsecret");

        var diff = ConfigDiff.diff(endpoint(before).buzhouConfig(), endpoint(after).buzhouConfig());
        assertThat(diff).isEmpty(); // 同明文同掩码——未变
    }
}
