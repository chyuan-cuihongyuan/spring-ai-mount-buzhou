package io.github.chyuan_cuihongyuan.buzhou.core.config;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 启动阶段耗时读数（spec 823 / T1147，Spring Boot {@code ApplicationStartup}
 * StartupStep 思想）：装配期各阶段（如「stores 装配」「hook 链构建」「首次
 * 会话预热」）起止耗时留痕——「启动慢在哪一步」从日志考古变结构化读数。
 *
 * <p>用法：{@code Step step = timing.start("phase-x"); ...; step.end();}
 * 未 end 的步骤在快照中 duration=-1（进行中哨兵）；步骤封顶
 * {@value #MAX_STEPS}（超出丢弃并 truncated——有界纪律）；Clock 注入可测。
 * 纯读数面——不挂接装配流程（喂点归应用/装配侧）。
 */
public final class StartupPhaseTiming {

    /** 步骤数封顶。 */
    public static final int MAX_STEPS = 64;

    /** 单步骤读数（duration=-1 表示未结束）。 */
    public record StepTiming(String phase, long startMillis, long durationMillis) {
    }

    /** 进行中的步骤句柄（end 幂等——重复 end 只计首末）。 */
    public static final class Step {
        private final String phase;
        private final long startMillis;
        private volatile long endMillis = -1;
        private final StartupPhaseTiming owner;

        private Step(StartupPhaseTiming owner, String phase, long startMillis) {
            this.owner = owner;
            this.phase = phase;
            this.startMillis = startMillis;
        }

        public void end() {
            if (endMillis < 0) {
                endMillis = owner.clock.millis();
            }
        }
    }

    private final Clock clock;
    private final List<Step> steps = new CopyOnWriteArrayList<>();
    private volatile boolean truncated;

    public StartupPhaseTiming(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** 开始一个阶段步骤（空白 phase 忽略返回 null；超封顶丢弃并 truncated）。 */
    public Step start(String phase) {
        if (phase == null || phase.isBlank()) {
            return null;
        }
        if (steps.size() >= MAX_STEPS) {
            truncated = true;
            return null;
        }
        Step step = new Step(this, phase, clock.millis());
        steps.add(step);
        return step;
    }

    /** 只读快照（按开始时刻升序；未结束步骤 duration=-1）。 */
    public List<StepTiming> snapshot() {
        List<StepTiming> out = new ArrayList<>(steps.size());
        for (Step step : steps) {
            long end = step.endMillis;
            out.add(new StepTiming(step.phase, step.startMillis,
                    end < 0 ? -1 : end - step.startMillis));
        }
        out.sort(Comparator.comparingLong(StepTiming::startMillis));
        return List.copyOf(out);
    }

    public boolean truncated() {
        return truncated;
    }
}
