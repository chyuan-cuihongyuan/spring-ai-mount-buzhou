package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BuzhouDemo} 行为断言（impl/05）：「人能跑」与「机器守得住」是同一份 demo ——
 * 本测试调用 {@code BuzhouDemo.run(...)}（即 {@code main} 跑的同一逻辑）并断言多轮 + 工具 + 微压缩 +
 * evidence 回查都成立，由默认 {@code mvn verify} 兜回归。
 */
class BuzhouDemoTest {

    @Test
    void run_compactsOldToolResultsAndEvidenceLookupReturnsOriginal() {
        BuzhouDemo.StubChatModel stub = new BuzhouDemo.StubChatModel("继续排查中");
        BuzhouDemo.DemoResult result = BuzhouDemo.run(stub);

        // 多轮 + 工具：本轮模型有回复、demo 跑通
        assertThat(result.reply()).isEqualTo("继续排查中");
        assertThat(result.transcript()).contains(BuzhouDemo.ORDER_ID);

        // 微压缩：旧轮大工具返回被替换为 evidence 占位符
        assertThat(result.compacted())
                .as("预置 10 轮大工具返回历史应触发微压缩（evidence 占位符）")
                .isTrue();
        assertThat(result.transcript()).contains("旧工具结果已清理");

        // evidence 回查：占位符的 evidence-id 经 read_evidence 取回原文（含订单号 —— P0 锚定）
        assertThat(result.evidenceId()).isNotNull();
        assertThat(result.evidenceOriginal())
                .as("evidence 回查应返回原文")
                .contains(BuzhouDemo.ORDER_ID)
                .contains(BuzhouDemo.ERROR_CODE);
    }
}
