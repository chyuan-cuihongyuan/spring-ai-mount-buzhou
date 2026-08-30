package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 163 / T524：热重载回归——替换生效+版本+通知 / 退订停收 / 条件换 /
 * 同值也计版本 / fail-fast / 多订阅全收。
 */
class ReloadableConfigTest {

    @Test
    void replaceTakesEffectAndNotifies() {
        ReloadableConfig<String> config = ReloadableConfig.of("v1");
        List<String> seen = new CopyOnWriteArrayList<>();
        config.subscribe(seen::add);

        assertThat(config.current()).isEqualTo("v1");
        assertThat(config.version()).isZero();

        config.replace("v2");
        assertThat(config.current()).isEqualTo("v2");
        assertThat(config.version()).isEqualTo(1);
        assertThat(seen).containsExactly("v2");
    }

    @Test
    void sameValueReplaceStillBumpsVersion() {
        ReloadableConfig<Integer> config = ReloadableConfig.of(7);
        config.replace(7);
        assertThat(config.version()).isEqualTo(1); // 重放安全——版本幂等依据
        assertThat(config.current()).isEqualTo(7);
    }

    @Test
    void unsubscribeStopsNotifications() {
        ReloadableConfig<String> config = ReloadableConfig.of("a");
        List<String> seen = new CopyOnWriteArrayList<>();
        Runnable unsubscribe = config.subscribe(seen::add);

        config.replace("b");
        unsubscribe.run();
        config.replace("c");

        assertThat(seen).containsExactly("b");
        assertThat(config.current()).isEqualTo("c");
    }

    @Test
    void updateIfTransformsCurrent() {
        ReloadableConfig<Long> config = ReloadableConfig.of(10L);
        config.updateIf(current -> current * 2);
        assertThat(config.current()).isEqualTo(20L);
        config.updateIf(current -> current + 5);
        assertThat(config.current()).isEqualTo(25L);
        assertThat(config.version()).isEqualTo(2);
    }

    @Test
    void multipleListenersAllNotified() {
        ReloadableConfig<String> config = ReloadableConfig.of("x");
        List<String> one = new CopyOnWriteArrayList<>();
        List<String> two = new CopyOnWriteArrayList<>();
        config.subscribe(one::add);
        config.subscribe(two::add);

        config.replace("y");
        assertThat(one).containsExactly("y");
        assertThat(two).containsExactly("y");
    }

    @Test
    void nullsFailFast() {
        assertThatThrownBy(() -> ReloadableConfig.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        ReloadableConfig<String> config = ReloadableConfig.of("a");
        assertThatThrownBy(() -> config.replace(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.updateIf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.subscribe(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.updateIf(current -> null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
