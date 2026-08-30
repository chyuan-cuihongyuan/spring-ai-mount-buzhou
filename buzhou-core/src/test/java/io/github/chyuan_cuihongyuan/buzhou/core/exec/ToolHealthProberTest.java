package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 165 / T528：工具健康探测回归——探针 UP / 异常与 false DOWN 连败 /
 * 恢复清零 / 翻转才通知 / 覆盖注册与参数校验。
 */
class ToolHealthProberTest {

    @Test
    void healthyProbeReportsUpWithoutNotifications() {
        ToolHealthProber prober = new ToolHealthProber();
        List<String> flips = new ArrayList<>();
        prober.onChange((tool, status) -> flips.add(tool + ":" + status.status()));
        prober.register("search", () -> true);

        Map<String, ToolHealthProber.ToolStatus> first = prober.probeOnce();
        assertThat(first.get("search").status()).isEqualTo(ToolHealthProber.Status.UP);
        prober.probeOnce(); // 再探仍 UP——无翻转
        assertThat(flips).isEmpty(); // 首探不通知（无旧态可比）
    }

    @Test
    void failingProbeAccumulatesConsecutiveDown() {
        ToolHealthProber prober = new ToolHealthProber();
        prober.register("fetch", () -> {
            throw new IllegalStateException("downstream unreachable");
        });

        prober.probeOnce();
        Map<String, ToolHealthProber.ToolStatus> second = prober.probeOnce();
        assertThat(second.get("fetch").status()).isEqualTo(ToolHealthProber.Status.DOWN);
        assertThat(second.get("fetch").consecutiveDown()).isEqualTo(2);
    }

    @Test
    void recoveryResetsConsecutiveCount() {
        ToolHealthProber prober = new ToolHealthProber();
        AtomicBoolean healthy = new AtomicBoolean(false);
        prober.register("flaky", healthy::get);

        prober.probeOnce(); // DOWN 1
        healthy.set(true);
        Map<String, ToolHealthProber.ToolStatus> recovered = prober.probeOnce();
        assertThat(recovered.get("flaky").status()).isEqualTo(ToolHealthProber.Status.UP);
        assertThat(recovered.get("flaky").consecutiveDown()).isZero();
    }

    @Test
    void notificationsFireOnlyOnFlips() {
        ToolHealthProber prober = new ToolHealthProber();
        List<String> flips = new ArrayList<>();
        prober.onChange((tool, status) -> flips.add(tool + ":" + status.status()));
        AtomicBoolean healthy = new AtomicBoolean(true);
        prober.register("db", healthy::get);

        prober.probeOnce(); // UP（首探——不通知）
        healthy.set(false);
        prober.probeOnce(); // → DOWN 翻转
        prober.probeOnce(); // 仍 DOWN——不通知
        healthy.set(true);
        prober.probeOnce(); // → UP 翻转

        assertThat(flips).containsExactly("db:DOWN", "db:UP");
    }

    @Test
    void reRegisterOverwritesAndArgumentsValidate() {
        ToolHealthProber prober = new ToolHealthProber();
        prober.register("tool", () -> false);
        prober.register("tool", () -> true); // 覆盖
        assertThat(prober.probeOnce().get("tool").status())
                .isEqualTo(ToolHealthProber.Status.UP);

        assertThatThrownBy(() -> prober.register(" ", () -> true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> prober.register("t", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ToolHealthProber().probeOnce()).isEmpty(); // 无探针空聚合
    }
}
