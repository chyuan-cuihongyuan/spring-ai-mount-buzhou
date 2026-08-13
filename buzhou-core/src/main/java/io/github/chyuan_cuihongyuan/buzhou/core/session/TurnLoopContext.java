package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;

/**
 * Turn 内 think→tool 递归循环的只读快照（wayfinder T17 / docs/spec/11 core）。
 *
 * <p>注意与 {@link io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext} 区分：后者描述
 * 一次用户 Turn（输入→最终回复）；本接口描述 Turn <b>内部</b> 工具递归循环的某一步，
 * 供停止条件（{@link TurnLoopPolicy}）裁决。
 */
public interface TurnLoopContext {

    /** 已执行完成的 think→tool 轮数（工具批已跑完并入历史）。 */
    int executedToolRounds();

    /** 即将执行的轮次编号（从 1 起）；停止条件在此轮<b>执行前</b>裁决。 */
    int nextToolRound();

    /** 本轮工具递归循环已耗时（自首次模型调用起）。 */
    Duration elapsed();

    String sessionId();

    String agentName();
}
