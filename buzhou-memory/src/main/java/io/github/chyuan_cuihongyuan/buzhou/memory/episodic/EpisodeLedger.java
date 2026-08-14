package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 情景记忆台账（wayfinder2 impl-26 / T42 / docs/spec/12 §memory-14，LangGraph/LangChain
 * episodic 语义）：成功任务经验（goal + 工具轨迹摘要 + 结局）存为 episode、
 * 以 goal 向量召回 top-k、按预算渲染为新任务「过往成功示例」few-shot 块——默认关。
 */
public final class EpisodeLedger {

    /** 单条情景（state 持久化：episode.&lt;seq&gt;）。 */
    public record Episode(String goal, String toolTraceDigest, String outcome, float[] vector) {
    }

    /** 召回命中。 */
    public record Example(String goal, String toolTraceDigest, String outcome, double score) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String KEY_PREFIX = "episode.";

    private final SessionStateStore stateStore;
    private final EmbeddingProvider provider;

    public EpisodeLedger(SessionStateStore stateStore, EmbeddingProvider provider) {
        this.stateStore = stateStore;
        this.provider = provider;
    }

    /**
     * 记录一次成功经验（任务成功判定后调用；或 sleep-time 整理器蒸馏）。
     * impl-38 / spec 13 §growth-8：序号从持久状态恢复——每次记录从该会话既有
     * {@code episode.<n>} 键推导下一序号（重启不归零、不覆盖既有情景）。
     */
    public synchronized void record(String sessionId, String goal, String toolTraceDigest,
                                     String outcome) {
        if (provider == null || goal == null || goal.isBlank()) {
            return;
        }
        try {
            int sequence = nextSequence(sessionId);
            String payload = MAPPER.writeValueAsString(Map.of(
                    "goal", goal,
                    "toolTraceDigest", toolTraceDigest == null ? "" : toolTraceDigest,
                    "outcome", outcome == null ? "success" : outcome));
            stateStore.put(sessionId, new StateEntry(KEY_PREFIX + sequence, payload,
                    "EpisodeLedger", 0, null, Instant.now()));
        } catch (Exception ignored) {
            // 情景记忆是增益非主链路：持久化失败不外溢
        }
    }

    /** 既有 episode 键的最大序号 + 1（持久状态恢复；坏键跳过不拖垮记录）。 */
    private int nextSequence(String sessionId) {
        int max = 0;
        for (String key : stateStore.getAll(sessionId).keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                try {
                    max = Math.max(max, Integer.parseInt(key.substring(KEY_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    // 非 .<n> 形状的键跳过
                }
            }
        }
        return max + 1;
    }

    /** 按新任务 goal 召回 top-k 过往成功示例（余弦）。 */
    public List<Example> recallExamples(String sessionId, String goal, int k) {
        if (provider == null || goal == null || goal.isBlank()) {
            return List.of();
        }
        float[] goalVector = provider.embed(goal);
        List<Example> examples = new ArrayList<>();
        stateStore.getAll(sessionId).forEach((key, entry) -> {
            if (!key.startsWith(KEY_PREFIX)) {
                return;
            }
            try {
                var node = MAPPER.readTree(entry.value());
                Episode episode = new Episode(node.path("goal").asText(),
                        node.path("toolTraceDigest").asText(), node.path("outcome").asText(),
                        null);
                float[] vector = provider.embed(episode.goal());
                examples.add(new Example(episode.goal(), episode.toolTraceDigest(),
                        episode.outcome(), EmbeddingProvider.cosine(goalVector, vector)));
            } catch (Exception ignored) {
                // 单条损坏不拖垮召回
            }
        });
        return examples.stream()
                .filter(example -> example.score() >= 0.10) // 语义地板：噪声级重叠不算命中
                .sorted(Comparator.comparingDouble(Example::score).reversed())
                .limit(Math.max(1, k))
                .toList();
    }

    /** few-shot 注入块（按预算截断；无命中 = empty）。 */
    public java.util.Optional<String> fewShotBlock(String sessionId, String goal, int k,
                                                   int maxChars) {
        List<Example> examples = recallExamples(sessionId, goal, k);
        if (examples.isEmpty()) {
            return java.util.Optional.empty();
        }
        StringBuilder block = new StringBuilder("[过往成功示例（情景记忆 few-shot）]");
        int budget = Math.max(200, maxChars);
        for (Example example : examples) {
            String line = "\n- 目标：" + example.goal() + "｜轨迹：" + example.toolTraceDigest()
                    + "｜结局：" + example.outcome();
            if (block.length() + line.length() > budget) {
                break;
            }
            block.append(line);
        }
        return java.util.Optional.of(block.toString());
    }
}
