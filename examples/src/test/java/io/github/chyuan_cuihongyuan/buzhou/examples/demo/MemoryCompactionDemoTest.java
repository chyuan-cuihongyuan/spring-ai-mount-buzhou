package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.TroubleshootingFixture;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillGuardModule;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 簇 1 · 记忆压缩链 + Spill 触发与回读（ticket 21 排障 demo）。
 *
 * <p>排障 Agent 长会话天然触发两级压缩与溢出保护，三个测试各侧重一环：
 * <ul>
 *   <li>{@link #summaryP0AnchoredAfterLargeHistory}：20+ 轮历史超预算触发九段摘要，P0 三段锚定预埋要点。</li>
 *   <li>{@link #microCompactionReplacesOldToolReturns}：无摘要场景下旧轮大工具返回被微压缩为 evidence 占位符，
 *       纯微压缩不产新代摘要；占位符的 evidence-id 经回查工具取回原文（与持久层一致）。</li>
 *   <li>{@link #spillOffloadsAndReadRangeRetrieves}：单条大日志超阈值自动落盘，上下文留预览 + spill 句柄，
 *       模型持 read_range 按需回读原文（动态解析句柄）。</li>
 * </ul>
 */
class MemoryCompactionDemoTest {

    @TempDir
    Path spillDir;

    @Test
    void summaryP0AnchoredAfterLargeHistory() {
        ScriptedChatModel main = new ScriptedChatModel();
        ScriptedChatModel summary = new ScriptedChatModel();
        summary.enqueue(new AssistantMessage(TroubleshootingFixture.NINE_SECTIONS));
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "demo-summary";
        stores.messageStore().append(sid, TroubleshootingFixture.troubleshootingHistory(sid, 20));

        RuntimeConfig config = MemoryModule.configure(
                TroubleshootingFixture.smallWindowYml(), stores, main, summary);
        AgentRuntime runtime = Buzhou.runtime(main, stores, config);

        main.enqueue(new AssistantMessage("继续排查"));
        AgentSession session = runtime.spawn("app", "agent", sid);
        session.chat("继续");
        session.close();

        String injected = main.seenPrompts.get(0).getInstructions().toString();
        // 摘要 P0 锚定：注入视图含九段 USER_INTENT + 订单号 + NEXT_STEP
        assertThat(injected).contains("<system-reminder>");
        assertThat(injected).contains("USER_INTENT").contains(TroubleshootingFixture.ORDER_ID);
        assertThat(injected).contains("NEXT_STEP");
        // 摘要落库（跨实例续接可凭 sessionId 取回）
        assertThat(stores.summaryStore().latest(sid)).isPresent();
    }

    @Test
    void microCompactionReplacesOldToolReturns() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "demo-micro";
        stores.messageStore().append(sid, TroubleshootingFixture.troubleshootingHistory(sid, 10));

        // 无摘要模型 + 默认大窗口：只触发微压缩，旧轮大工具返回替换为 evidence 占位符
        RuntimeConfig config = MemoryModule.configure(Map.of(), stores.messageStore());
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(new AssistantMessage("继续"));
        AgentSession session = runtime.spawn("app", "agent", sid);
        session.chat("继续");
        session.close();

        Prompt firstCall = model.seenPrompts.get(0);
        String injected = firstCall.getInstructions().toString();
        // 旧轮工具返回被微压缩为占位符
        assertThat(injected).contains("旧工具结果已清理").contains("evidence-id=");
        assertThat(toolNamesOf(firstCall)).contains("read_evidence");
        // 两级先后次序：无摘要模型 + 默认大窗口下只触发微压缩，不产新代摘要
        assertThat(stores.summaryStore().latest(sid)).as("纯微压缩不应产摘要").isEmpty();
        // evidence 回查：占位符的 evidence-id 经回查工具取回原文（与持久层一致）
        String evidenceId = extractEvidenceId(injected);
        assertThat(evidenceId).as("应能从占位符解析 evidence-id").isNotNull();
        String original = new EvidenceLookupTool(stores.messageStore())
                .call("{\"evidenceId\":\"" + evidenceId + "\"}");
        assertThat(original).as("evidence 回查应返回原文").contains(TroubleshootingFixture.ERROR_CODE);
    }

    @Test
    void spillOffloadsAndReadRangeRetrieves() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "demo-spill";
        String bigLog = "[网关日志] 错误 " + TroubleshootingFixture.ERROR_CODE + " " + "x".repeat(500);
        // spill 句柄末段是动态 id，ScriptedChatModel 无法预编排 read_range 的 path；
        // 继承 ScriptedChatModel 重写 call（复用 seenPrompts，动态解析注入视图里的 spill:// URI 再回读）
        String[] spillUri = new String[1];
        String[] readRangeView = new String[1];
        ScriptedChatModel model = new ScriptedChatModel() {
            int callNo = 0;

            @Override
            public ChatResponse call(Prompt prompt) {
                seenPrompts.add(prompt);
                callNo++;
                String view = prompt.getInstructions().toString();
                if (callNo == 1) {
                    return toolCall("query_logs", "{}");
                }
                if (callNo == 2) {
                    spillUri[0] = extractSpillUri(view);
                    return toolCall("read_range",
                            "{\"path\":\"" + spillUri[0] + "\",\"mode\":\"bytes\"}");
                }
                readRangeView[0] = view;
                return new ChatResponse(List.of(new Generation(new AssistantMessage("已回读日志"))));
            }
        };

        SpillModule spill = SpillModule.withDefaults(spillDir);
        // 阈值调小至 100：demo 不必造 32000 字符即可触发 spill
        RuntimeConfig config = RuntimeConfig.merge(
                MemoryModule.configure(Map.of("model-name", "demo-model"), stores, model, null),
                SpillGuardModule.fromModule(spill, spillDir).thresholdChars(100).build().configure());
        AgentRuntime runtime = Buzhou.runtime(model, stores, config,
                TroubleshootingFixture.fixedTool("query_logs", bigLog));

        AgentSession session = runtime.spawn("app", "agent", sid);
        session.chat("查网关日志");
        session.close();

        // spill 触发并注入句柄（原文不进上下文）
        assertThat(spillUri[0]).as("spill 句柄应注入第二轮").startsWith("spill://agent/" + sid + "/");
        // read_range 回读成功：第三轮注入视图含原文中的错误码
        assertThat(readRangeView[0]).contains(TroubleshootingFixture.ERROR_CODE);
    }

    private static List<String> toolNamesOf(Prompt prompt) {
        List<String> names = new ArrayList<>();
        if (prompt.getOptions() instanceof ToolCallingChatOptions t) {
            t.getToolCallbacks().forEach(cb -> names.add(cb.getToolDefinition().name()));
        }
        return names;
    }

    private static ChatResponse toolCall(String name, String args) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc", "function", name, args))).build())));
    }

    private static String extractSpillUri(String view) {
        Matcher m = Pattern.compile("spill://[\\w/-]+").matcher(view);
        if (!m.find()) {
            throw new AssertionError("未在注入视图找到 spill URI");
        }
        return m.group();
    }

    private static String extractEvidenceId(String view) {
        Matcher m = Pattern.compile("evidence-id=([\\w-]+)").matcher(view);
        return m.find() ? m.group(1) : null;
    }
}
