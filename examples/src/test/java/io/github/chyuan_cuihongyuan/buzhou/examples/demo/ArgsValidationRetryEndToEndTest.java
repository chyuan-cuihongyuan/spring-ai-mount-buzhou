package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-04 / T30（docs/spec/12 §core-2）端到端：工具参数未过 schema 时<b>不执行</b>、
 * 校验反馈回喂模型自愈重试；重试预算耗尽后循环 REASK_FAILED 优雅收尾。
 */
class ArgsValidationRetryEndToEndTest {

    private static final String SCHEMA = """
            {"type":"object","properties":{"orderId":{"type":"string"}},"required":["orderId"]}""";

    @Test
    void invalidArgsFeedBackAndModelSelfHeals() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("query_order", "{\"order_id\":\"O-1\"}"), // 错键名：缺必填 orderId
                ScriptStep.toolCall("query_order", "{\"orderId\":\"O-1\"}"), // 修正后成功
                ScriptStep.text("已查到订单 O-1 的状态"));
        FakeModelGuard.requireTestDouble(model);

        List<String> executedArgs = new CopyOnWriteArrayList<>();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults(), recordingTool("query_order", executedArgs));
        AgentSession session = runtime.spawn("args-app", "agent", "self-heal");
        String reply = session.chat("查订单");
        session.close();

        // 自愈闭环：第一次调用未执行（校验拦截），修正后执行成功
        assertThat(executedArgs).containsExactly("{\"orderId\":\"O-1\"}");
        assertThat(reply).isEqualTo("已查到订单 O-1 的状态");
        // 第一次回喂的确实是「校验失败」词汇（区别于执行失败）
        Message feedback = model.seenPrompts.get(1).getInstructions().getLast();
        assertThat(((ToolResponseMessage) feedback).getResponses().getFirst().responseData())
                .startsWith("[工具参数校验失败]")
                .contains("缺少必填字段「orderId」");
    }

    @Test
    void exhaustedRetryBudgetStopsGracefullyWithReaskFailed() {
        // 预算 1：第一次校验失败允许重试；第二次仍失败 → 超预算 → REASK_FAILED 优雅收尾
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("query_order", "{\"wrong\":1}"),
                ScriptStep.toolCall("query_order", "{\"still_wrong\":2}"),
                ScriptStep.toolCall("query_order", "{\"never\":3}"),
                ScriptStep.text("不应到达"));
        FakeModelGuard.requireTestDouble(model);

        List<String> executedArgs = new CopyOnWriteArrayList<>();
        RuntimeConfig config = RuntimeConfig.turnLoopPolicy(TurnLoopPolicy.of(10, 1));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), config,
                recordingTool("query_order", executedArgs));
        AgentSession session = runtime.spawn("args-app", "agent", "budget-exhausted");
        String reply = session.chat("查订单");
        session.close();

        // 工具从未被执行（全部被校验拦截）；循环以 REASK_FAILED 文案优雅终止
        assertThat(executedArgs).isEmpty();
        assertThat(reply).contains("重试预算上限").contains("校验失败");
        // 模型被调了三次：两次收到校验反馈；第三次仍要工具 → 被护栏替换为 REASK_FAILED 最终回复
        //（第 3 次调用的工具响应被替换丢弃，脚本第 4 步「不应到达」从未消费）
        assertThat(model.callCount()).isEqualTo(3);
    }

    private static ToolCallback recordingTool(String name, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema(SCHEMA).build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(toolInput);
                return "order-status: DELIVERED";
            }
        };
    }
}
