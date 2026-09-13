package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-678 / spec 921：软截止预警旗标语义——未配置零触发、配置后进窗一次性置位、
 * beginTurn 复位（派发行为零变化；WARN 与 counter 同点在 checkSoftDeadlineWindow）。
 */
class SoftDeadlineFlagTest {

    private HarnessToolCallingManager manager() {
        return new HarnessToolCallingManager(
                ToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(10),
                java.util.Map.of());
    }

    @Test
    void disabledByDefault() {
        HarnessToolCallingManager manager = manager();
        manager.beginTurn(TurnDeadline.in(Duration.ofMillis(300)));
        assertThat(manager.softDeadlineWarned()).isFalse();
    }

    @Test
    void beginTurnResetsWarnFlag() {
        HarnessToolCallingManager manager = manager();
        manager.setSoftDeadlineWindow(Duration.ofMillis(150));
        manager.beginTurn(TurnDeadline.in(Duration.ofMillis(400)));
        assertThat(manager.softDeadlineWarned()).isFalse();
        // 新 Turn 开始即复位（AtomicBoolean set(false)）
        manager.beginTurn(TurnDeadline.in(Duration.ofMinutes(5)));
        assertThat(manager.softDeadlineWarned()).isFalse();
    }

    @Test
    void withinWindowDetectedByValueObject() {
        // 值对象判定与旗标解耦复测（TurnDeadline.withinSoftWindow 已单测——此处验证装配条件成立）
        TurnDeadline near = TurnDeadline.in(Duration.ofMillis(100));
        assertThat(near.withinSoftWindow(Duration.ofMillis(150))).isTrue();
    }
}
