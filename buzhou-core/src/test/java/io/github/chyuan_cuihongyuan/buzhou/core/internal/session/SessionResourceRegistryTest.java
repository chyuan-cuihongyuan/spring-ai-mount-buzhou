package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ticket 29：closeAll 多异常收集行为测试——单个资源 close 失败不跳过其余清理，
 * 首个失败外抛、其余以 suppressed 附加（不静默吞）；幂等与关闭后拒绝注册语义不变。
 */
class SessionResourceRegistryTest {

    @Test
    void shouldAttachAllFailuresAsSuppressed_whenMultipleResourcesFailToClose() {
        SessionResourceRegistry registry = new SessionResourceRegistry();
        registry.register("first", () -> {
        });
        registry.register("second", () -> {
            throw new IllegalStateException("boom-second");
        });
        registry.register("third", () -> {
            throw new IllegalStateException("boom-third");
        });

        // LIFO 关闭：third 先失败 → 作为首个外抛；second 的失败 suppressed 附加（不静默吞）
        assertThatThrownBy(registry::closeAll)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("third")
                .satisfies(e -> {
                    assertThat(e.getSuppressed()).hasSize(1);
                    assertThat(e.getSuppressed()[0]).hasMessageContaining("second");
                });
    }

    @Test
    void shouldCloseRemainingResources_whenEarlierCloseFails() {
        SessionResourceRegistry registry = new SessionResourceRegistry();
        List<String> closed = new ArrayList<>();
        registry.register("ok-1", () -> closed.add("ok-1"));
        registry.register("bad", () -> {
            throw new IllegalStateException("boom");
        });
        registry.register("ok-2", () -> closed.add("ok-2"));

        assertThatThrownBy(registry::closeAll).hasMessageContaining("bad");

        // 失败之后注册的资源仍被关闭（不跳过其余清理）
        assertThat(closed).containsExactlyInAnyOrder("ok-1", "ok-2");
    }

    @Test
    void shouldCloseEachResourceOnlyOnce_whenCloseAllInvokedTwice() {
        SessionResourceRegistry registry = new SessionResourceRegistry();
        List<String> closed = new ArrayList<>();
        registry.register("once", () -> closed.add("once"));

        registry.closeAll();
        registry.closeAll();

        assertThat(closed).containsExactly("once");
        assertThat(registry.isClosed()).isTrue();
    }

    @Test
    void shouldRejectRegistration_whenRegistryAlreadyClosed() {
        SessionResourceRegistry registry = new SessionResourceRegistry();
        registry.closeAll();

        assertThatThrownBy(() -> registry.register("late", () -> {
        })).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("late");
    }

    @Test
    void shouldCloseWithoutFailure_whenAllResourcesSucceed() {
        SessionResourceRegistry registry = new SessionResourceRegistry();
        List<String> closed = new ArrayList<>();
        registry.register("a", () -> closed.add("a"));
        registry.register("b", () -> closed.add("b"));

        registry.closeAll();

        // LIFO 顺序关闭
        assertThat(closed).containsExactly("b", "a");
    }
}
