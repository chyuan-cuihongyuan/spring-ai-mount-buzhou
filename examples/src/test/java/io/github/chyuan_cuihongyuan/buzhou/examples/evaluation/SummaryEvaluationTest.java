package io.github.chyuan_cuihongyuan.buzhou.examples.evaluation;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.TroubleshootingFixture;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 摘要质量评测套件（ticket 21 · spec 01 评测方案）。
 *
 * <p>排障场景 20+ 轮混合大小工具返回会话，测四指标并打印报告；阈值断言随 {@code mvn verify} 作 CI 回归门禁。
 *
 * <h3>四指标（CI 跑确定性部分）</h3>
 * <ul>
 *   <li><b>P0 信息保留率 ≥ 95%</b>：摘要块 USER_INTENT / CURRENT_STATE / NEXT_STEP 三段含预埋要点。</li>
 *   <li><b>关键事实召回率 ≥ 90%</b>：预埋事实探针（订单号 / 错误码 / 流水号）在摘要块的命中率。</li>
 *   <li><b>token 压缩率 ≤ 40%</b>：压缩后注入视图 token / 原始全量历史 token（字符启发式）。</li>
 *   <li><b>任务续接</b>：压缩后续跑一轮，目标（意图 + 订单号）仍在注入视图。</li>
 * </ul>
 *
 * <h3>脚本化摘要下的指标含义（caveat）</h3>
 * CI 用 {@link ScriptedChatModel} 驱动，摘要内容由脚本给定，故 P0/召回验证的是「压缩管道触发 + 摘要块注入含
 * 要点」（管道正确性），<b>而非</b>真实压缩保留质量——后者需离线 LLM-as-judge（见下）。为避免「未压缩也通过」，
 * 本测试先断言 {@code summaryStore.latest} 已生成、摘要块已注入，确保压缩确已触发，保留率指标方有意义；
 * 且 P0/召回只比对<b>摘要块</b>（system-reminder），不混入近期原文。
 *
 * <h3>LLM-as-judge 方法论（不在 CI 强制）</h3>
 * CI 无真实 LLM，故判官模型与人工抽检作为方法论文档：判官模型与被测模型<b>不同源</b>（避免系统性偏差），
 * 输入 {原始全量历史, 压缩后注入视图, 预埋要点清单, 续跑记录}，要求输出 JSON
 * {@code {p0_hit:[{item,retained,evidence}], continuation_score:1-5, fact_recall:[{probe,recalled}], rationale}}；
 * 每个用例 judge 结果抽样 20% 人工校准。本地或离线评测时填入判官模型即可启用。
 */
class SummaryEvaluationTest {

    private static final String ORDER_ID = TroubleshootingFixture.ORDER_ID;
    private static final String ERROR_CODE = TroubleshootingFixture.ERROR_CODE;
    private static final String PAYMENT_REF = TroubleshootingFixture.PAYMENT_REF;

    @Test
    void fourMetricsMeetThresholdsAndPrintReport() {
        ScriptedChatModel main = new ScriptedChatModel();
        ScriptedChatModel summary = new ScriptedChatModel();
        // 首次压缩 + 可能的续接增量合并，各预置九段（被测摘要由脚本驱动）
        summary.enqueue(new AssistantMessage(TroubleshootingFixture.NINE_SECTIONS));
        summary.enqueue(new AssistantMessage(TroubleshootingFixture.NINE_SECTIONS));

        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "eval-summary";
        List<BuzhouMessage> history = TroubleshootingFixture.troubleshootingHistory(sid, 20);
        stores.messageStore().append(sid, history);

        RuntimeConfig config = MemoryModule.configure(
                TroubleshootingFixture.smallWindowYml(), stores, main, summary);
        AgentRuntime runtime = Buzhou.runtime(main, stores, config);

        AgentSession session = runtime.spawn("app", "agent", sid);
        main.enqueue(new AssistantMessage("继续排查"));
        session.chat("继续");
        // 续接：压缩后再跑一轮，验证目标不丢
        main.enqueue(new AssistantMessage("继续推进"));
        session.chat("第 21 轮");
        session.close();

        String compressedView = main.seenPrompts.get(0).getInstructions().toString();
        String continuedView = main.seenPrompts.get(1).getInstructions().toString();
        String summaryBlock = summaryBlock(compressedView);

        // 前提：压缩确已触发（摘要生成 + 摘要块注入），否则保留率指标无意义
        assertThat(stores.summaryStore().latest(sid)).as("压缩应触发摘要生成").isPresent();
        assertThat(summaryBlock).as("摘要块应注入 system-reminder").isNotEmpty();

        double p0 = p0Retention(summaryBlock);
        double recall = factRecall(summaryBlock);
        double compression = compressionRatio(history, compressedView);
        boolean continuationOk = continuedView.contains("USER_INTENT") && continuedView.contains(ORDER_ID);

        System.out.println("\n===== 排障会话摘要评测报告（ticket 21 · 四指标）=====");
        System.out.printf("P0 信息保留率 : %.0f%%  (阈值 ≥ 95%%，比对摘要块)%n", p0 * 100);
        System.out.printf("关键事实召回率: %.0f%%  (阈值 ≥ 90%%，比对摘要块)%n", recall * 100);
        System.out.printf("token 压缩率  : %.0f%%  (阈值 ≤ 40%%)%n", compression * 100);
        System.out.printf("续接目标保留  : %s  (压缩后续跑，意图+订单号仍在)%n", continuationOk ? "通过" : "失败");
        System.out.println("注：CI 脚本化摘要下 P0/召回验证管道正确性；真实保留质量 + LLM-as-judge + 20% 人工抽检见 javadoc/README（CI 不强制）");
        System.out.println("=====================================================\n");

        // CI 回归门禁
        assertThat(p0).as("P0 信息保留率 ≥ 95%").isGreaterThanOrEqualTo(0.95);
        assertThat(recall).as("关键事实召回率 ≥ 90%").isGreaterThanOrEqualTo(0.90);
        assertThat(compression).as("token 压缩率 ≤ 40%").isLessThanOrEqualTo(0.40);
        assertThat(continuationOk).as("续接后目标（意图+订单号）仍在注入视图").isTrue();
    }

    /** P0 三段（意图/现场/下一步）在摘要块中逐条比对预埋要点 → 命中率。 */
    private static double p0Retention(String summaryBlock) {
        int hits = 0;
        if (summaryBlock.contains("USER_INTENT") && summaryBlock.contains(ORDER_ID)) {
            hits++;
        }
        if (summaryBlock.contains("CURRENT_STATE") && summaryBlock.contains(ERROR_CODE)) {
            hits++;
        }
        if (summaryBlock.contains("NEXT_STEP")) {
            hits++;
        }
        return hits / 3.0;
    }

    /** 预埋事实探针在摘要块中的命中率。 */
    private static double factRecall(String summaryBlock) {
        int hits = 0;
        if (summaryBlock.contains(ORDER_ID)) {
            hits++;
        }
        if (summaryBlock.contains(ERROR_CODE)) {
            hits++;
        }
        if (summaryBlock.contains(PAYMENT_REF)) {
            hits++;
        }
        return hits / 3.0;
    }

    /**
     * 压缩后注入视图 token / 原始全量历史 token。分子 = 模型实际所见的完整注入视图（含系统提示 / 摘要块 /
     * 近期原文），分母 = 原始历史消息 content 之和；反映「模型上下文体积 vs 不压缩时的历史体积」（spec 01 定义）。
     */
    private static double compressionRatio(List<BuzhouMessage> history, String injected) {
        int historyTokens = history.stream()
                .mapToInt(m -> TroubleshootingFixture.estimateTokens(m.content() == null ? "" : m.content()))
                .sum();
        int injectedTokens = TroubleshootingFixture.estimateTokens(injected);
        return (double) injectedTokens / Math.max(1, historyTokens);
    }

    /** 提取注入视图里的 system-reminder 摘要块（压缩后摘要所在）；无则空串。 */
    private static String summaryBlock(String injected) {
        int start = injected.indexOf("<system-reminder>");
        int end = injected.indexOf("</system-reminder>");
        if (start < 0 || end < 0 || end <= start) {
            return "";
        }
        return injected.substring(start, end);
    }
}
