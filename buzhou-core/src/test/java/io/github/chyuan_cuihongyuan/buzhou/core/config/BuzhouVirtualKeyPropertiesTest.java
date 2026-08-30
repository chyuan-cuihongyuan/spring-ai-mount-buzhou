package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 158 §B / T511：虚拟 key 属性红队——limits 值 ≥1 bind 期 fail-fast；
 * active-key 空白归一为 null（省缺 = 不启用）；active-key 不在 limits 的装配期
 * 校验留给 autoconfig（矩阵逐键绑定不炸——分工入档）。
 */
class BuzhouVirtualKeyPropertiesTest {

    @Test
    void defaultsAreInertAndBlankKeyNormalizes() {
        BuzhouVirtualKeyProperties props = new BuzhouVirtualKeyProperties(null, null);
        assertThat(props.limits()).isNull();
        assertThat(props.activeKey()).isNull();

        assertThat(new BuzhouVirtualKeyProperties(Map.of(), " ").activeKey()).isNull();
    }

    @Test
    void nonPositiveLimitFailsFastAtBind() {
        assertThatThrownBy(() -> new BuzhouVirtualKeyProperties(
                Map.of("app-key", 0L), "app-key"))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("limits.app-key");
        assertThatThrownBy(() -> new BuzhouVirtualKeyProperties(
                Map.of("app-key", -5L), null))
                .isInstanceOf(BuzhouConfigurationException.class);
    }

    @Test
    void validTableBinds() {
        BuzhouVirtualKeyProperties props = new BuzhouVirtualKeyProperties(
                Map.of("app-key", 1_000_000L), "app-key");
        assertThat(props.limits()).containsEntry("app-key", 1_000_000L);
        assertThat(props.activeKey()).isEqualTo("app-key");
    }
}
