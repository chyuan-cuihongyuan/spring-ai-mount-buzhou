package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
import java.util.function.Function;

public interface MicroCompactor {

    /**
     * 微压缩：把可回收的旧工具结果替换为 evidence 占位符。
     *
     * @param evictRatio 逐出比例（impl-02 / T36，Letta「evict only a portion (~70%)」）：
     *                   只逐出<b>最旧</b>的 {@code ceil(候选数 × ratio)} 条，保留最新 (1-ratio) 原文
     *                   续接——部分逐出保连续；{@code 1.0} 即全量逐出（旧行为）。
     */
    MicroCompactionResult compact(List<BuzhouMessage> history,
                                  int currentTurnIndex,
                                  Function<String, MicroCompactionPolicy> policyByToolName,
                                  int protectRecentTurns,
                                  double evictRatio);

    /** 兼容重载：全量逐出（ratio=1.0，既有调用方/测试行为不变）。 */
    default MicroCompactionResult compact(List<BuzhouMessage> history,
                                          int currentTurnIndex,
                                          Function<String, MicroCompactionPolicy> policyByToolName,
                                          int protectRecentTurns) {
        return compact(history, currentTurnIndex, policyByToolName, protectRecentTurns, 1.0);
    }
}
