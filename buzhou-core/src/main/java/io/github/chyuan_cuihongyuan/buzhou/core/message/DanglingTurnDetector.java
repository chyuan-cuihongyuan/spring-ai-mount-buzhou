package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 悬空轮检测器（spec 1439 / T2179 / impl 1091）——Temporal activity 检测
 * 思想（中断后「activity 无结果」是恢复语义的边界形态）：会话被取消/中断/
 * 崩溃重启后，history 里可能留下**有 USER 输入但无 ASSISTANT 回复**的悬空轮
 * ——续聊时模型看到「自己被问了却没答」的历史，回复质量与上下文一致性受损。
 * 本检测器按 turnSeq 分组显形悬空轮。
 *
 * <p>纯函数零状态：单会话 history 口径（调用方按会话过滤后喂入）。
 * 判定口径：轮内有 USER 且无 ASSISTANT 即悬空（TOOL 不影响判定——工具链
 * 无 ASSISTANT 收尾同样是悬空形态）；轮内仅有 TOOL/SYSTEM 不算悬空
 * （外部写入形态，非「问了没答」）。
 */
public final class DanglingTurnDetector {

    /** 样本榜容量（悬空轮 turnSeq 升序取前 N——定位用）。 */
    static final int SAMPLE_CAPACITY = 8;

    private DanglingTurnDetector() {
    }

    /**
     * @param totalTurns       出现过的轮次数（按 turnSeq 去重）
     * @param danglingTurnCount 悬空轮数（有 USER 无 ASSISTANT）
     * @param danglingSamples   悬空轮 turnSeq 样本（升序，封顶 {@value #SAMPLE_CAPACITY}）
     */
    public record Report(int totalTurns, int danglingTurnCount, List<Integer> danglingSamples) {

        /** 是否存在悬空轮（续聊质量风险哨兵）。 */
        public boolean hasDangling() {
            return danglingTurnCount > 0;
        }
    }

    /** 检测入口：单会话 history（顺序无关，按 turnSeq 分组）。 */
    public static Report analyze(List<BuzhouMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new Report(0, 0, List.of());
        }
        Map<Integer, TreeSet<Role>> turns = new LinkedHashMap<>();
        for (BuzhouMessage m : messages) {
            turns.computeIfAbsent(m.turnSeq(), k -> new TreeSet<>())
                    .add(m.role() == null ? Role.SYSTEM : m.role());
        }
        List<Integer> dangling = new ArrayList<>();
        for (Map.Entry<Integer, TreeSet<Role>> e : turns.entrySet()) {
            if (e.getValue().contains(Role.USER) && !e.getValue().contains(Role.ASSISTANT)) {
                dangling.add(e.getKey());
            }
        }
        List<Integer> samples = dangling.stream()
                .limit(SAMPLE_CAPACITY)
                .toList();
        return new Report(turns.size(), dangling.size(), List.copyOf(samples));
    }
}
