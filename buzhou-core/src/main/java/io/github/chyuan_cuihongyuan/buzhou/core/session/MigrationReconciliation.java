package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会话迁移对账（spec 825 / T1151，gh-ost 在线迁移行数/校验和对账思想）：
 * 迁移后对源/目标两份 {@link SessionExport} 做计数+边界+状态键三维对账——
 * 「搬完了」不等于「搬对了」；消息数不符/轮次边界漂移/状态键缺失结构化列出。
 *
 * <p>纯函数：入参两份导出（源在迁移前导出、目标在迁移后导出）；sessionId
 * 不比对（默认重映射语义下源≠目标——诚实口径）；mismatch 明细封顶
 * {@value #MAX_MISMATCHES}（超出以 {@code …} 汇总行截断）。
 */
public final class MigrationReconciliation {

    /** 对账报告（不可变）。 */
    public record Reconciliation(long sourceMessages, long targetMessages,
                                 long sourceTurns, long targetTurns,
                                 int sourceStateKeys, int targetStateKeys,
                                 boolean countsMatch, List<String> mismatches) {
    }

    /** mismatch 明细封顶。 */
    public static final int MAX_MISMATCHES = 8;

    private MigrationReconciliation() {
    }

    /** 对账：消息数/消息 id 首尾边界/轮次范围/状态键集合 四维。 */
    public static Reconciliation verify(SessionExport source, SessionExport target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");

        List<String> mismatches = new ArrayList<>();
        long srcMsgs = source.messages() == null ? 0 : source.messages().size();
        long tgtMsgs = target.messages() == null ? 0 : target.messages().size();
        if (srcMsgs != tgtMsgs) {
            mismatches.add("消息数不符：源 " + srcMsgs + " vs 目标 " + tgtMsgs);
        }

        long[] srcTurns = turnRange(source.messages());
        long[] tgtTurns = turnRange(target.messages());
        if (srcTurns[0] != tgtTurns[0] || srcTurns[1] != tgtTurns[1]) {
            mismatches.add("轮次范围漂移：源 [" + srcTurns[0] + "," + srcTurns[1]
                    + "] vs 目标 [" + tgtTurns[0] + "," + tgtTurns[1] + "]");
        }

        // 首尾消息 id 边界（重映射下中间 id 可能变——首尾 + 计数是最小充分边界）
        String srcFirst = messageIdAt(source.messages(), 0);
        String tgtFirst = messageIdAt(target.messages(), 0);
        String srcLast = messageIdAt(source.messages(), (int) Math.max(0, srcMsgs - 1));
        String tgtLast = messageIdAt(target.messages(), (int) Math.max(0, tgtMsgs - 1));
        if (!Objects.equals(srcFirst, tgtFirst) && keepIds(source, target)) {
            mismatches.add("首条消息 id 漂移：" + srcFirst + " vs " + tgtFirst);
        }
        if (!Objects.equals(srcLast, tgtLast) && keepIds(source, target)) {
            mismatches.add("末条消息 id 漂移：" + srcLast + " vs " + tgtLast);
        }

        int srcKeys = source.state() == null ? 0 : source.state().size();
        int tgtKeys = target.state() == null ? 0 : target.state().size();
        if (srcKeys != tgtKeys) {
            mismatches.add("状态键数不符：源 " + srcKeys + " vs 目标 " + tgtKeys);
        }
        List<String> missingKeys = missingStateKeys(source.state(), target.state());
        if (!missingKeys.isEmpty()) {
            mismatches.add("目标缺失状态键：" + String.join(",", truncated(missingKeys)));
        }

        return new Reconciliation(srcMsgs, tgtMsgs, srcTurns[1] - srcTurns[0] + (srcMsgs == 0 ? 0 : 1),
                tgtTurns[1] - tgtTurns[0] + (tgtMsgs == 0 ? 0 : 1),
                srcKeys, tgtKeys, mismatches.isEmpty(), List.copyOf(mismatches));
    }

    private static boolean keepIds(SessionExport source, SessionExport target) {
        return Objects.equals(source.sessionId(), target.sessionId());
    }

    private static String messageIdAt(List<BuzhouMessage> messages, int index) {
        if (messages == null || index < 0 || index >= messages.size()) {
            return null;
        }
        return messages.get(index).id();
    }

    private static long[] turnRange(List<BuzhouMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new long[]{0, 0};
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (BuzhouMessage m : messages) {
            if (m == null) {
                continue;
            }
            min = Math.min(min, m.turnSeq());
            max = Math.max(max, m.turnSeq());
        }
        if (min == Long.MAX_VALUE) {
            return new long[]{0, 0};
        }
        return new long[]{min, max};
    }

    private static List<String> missingStateKeys(Map<String, StateEntry> source, Map<String, StateEntry> target) {
        List<String> missing = new ArrayList<>();
        if (source == null) {
            return missing;
        }
        for (String key : source.keySet()) {
            if (target == null || !target.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    private static List<String> truncated(List<String> items) {
        if (items.size() <= MAX_MISMATCHES) {
            return items;
        }
        List<String> cut = new ArrayList<>(items.subList(0, MAX_MISMATCHES));
        cut.add("…（共 " + items.size() + " 项）");
        return cut;
    }
}
