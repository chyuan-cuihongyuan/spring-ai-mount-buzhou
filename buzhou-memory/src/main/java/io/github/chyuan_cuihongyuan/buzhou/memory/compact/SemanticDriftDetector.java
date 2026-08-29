package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

/**
 * 语义漂移检测器（spec 90 §A / T343，Letta 语义触发压缩 / Phoenix embedding 漂移
 * 思想）：当前输入与既有摘要的话题是否已漂移——漂移 = 旧话题轮次近期大概率不再
 * 被引用，是提前摘要的天然时机（比积压计数更贴近「自然边界」）。
 *
 * <p>实现自由（词面/嵌入/模型）；宿主可注入 embedding 版。默认
 * {@link LexicalDriftDetector}（字符 bigram Jaccard——零依赖，中英文通吃）。
 */
@FunctionalInterface
public interface SemanticDriftDetector {

    /**
     * @param currentInput 本轮用户输入文本
     * @param summaryText  既有摘要渲染文本（漂移基准）
     * @return true = 话题已漂移（建议边界提前压缩）
     */
    boolean drifted(String currentInput, String summaryText);
}
