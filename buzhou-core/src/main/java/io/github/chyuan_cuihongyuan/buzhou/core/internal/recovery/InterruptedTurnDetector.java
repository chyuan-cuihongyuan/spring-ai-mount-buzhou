package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;

import java.util.List;

/**
 * 「被中断轮次」判定（spec「崩溃中轮次恢复 / 恢复语义分档」）。
 *
 * <p>判定口径：加载（或加载 + 悬空修复）后的历史，<b>结尾没有终结性助手回复</b>——即最后一轮
 * 只到「助手发了工具调用 / 工具结果已落库（或经悬空修复补齐）」而<b>没有最终的、不含工具调用的
 * 助手消息</b>。完结轮次（已有终结回复）不算中断，不续跑。
 *
 * <p>两个崩溃窗口均落入「被中断」：
 * <ul>
 *   <li>窗口 A（悬空工具调用）：结尾是带工具调用的 ASSISTANT（结果未落库）→ 经
 *       {@code DanglingCallRepairer} 修复补齐；修复前最后一条即带 toolCalls 的 ASSISTANT。</li>
 *   <li>窗口 B（中断轮次无终结回复）：结尾是 TOOL 消息（工具结果已落库，无最终助手回复）。</li>
 * </ul>
 * 两种形态在<b>原始持久化历史</b>上都可直接判定（无需先跑修复器），故本检测用于 spawn 期
 * 的恢复决策，与 {@code BuzhouChatMemory.get()} 内的懒修复互不依赖。
 */
public final class InterruptedTurnDetector {

    private InterruptedTurnDetector() {
    }

    /**
     * @param stored 原始持久化历史（未压缩视图，按 turnSeq/seqInTurn 升序）
     * @return 最后一条消息表明该轮被中断（无终结性助手回复）时返回 {@code true}；空历史返回 {@code false}
     */
    public static boolean wasInterrupted(List<BuzhouMessage> stored) {
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        BuzhouMessage last = stored.get(stored.size() - 1);
        // 结尾是 TOOL：工具已执行（或经修复补齐），缺最终助手回复 → 中断（窗口 B）
        if (last.role() == Role.TOOL) {
            return true;
        }
        // 结尾是带工具调用的 ASSISTANT：调用未结束（缺响应） → 中断（窗口 A，修复前形态）
        return last.role() == Role.ASSISTANT && !last.toolCalls().isEmpty();
    }
}
