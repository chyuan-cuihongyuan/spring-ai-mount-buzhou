package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.spill.RangeReadRequest;
import io.github.chyuan_cuihongyuan.buzhou.spill.ReadRangeTool;
import io.github.chyuan_cuihongyuan.buzhou.spill.SessionReadOnlyRegistry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillGuardModule;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T20/T21 端到端（docs/spec/11 spill，主接缝 = agent session）：
 * hot-tail 模式下近期工具结果全量内联；变旧后视图级溢出为<b>自描述占位符</b>
 * （句柄 + 形状 + 大小 + 回读动词）；回读返回真实切片（非编造）。
 */
class SpillHotTailIntegrationTest {

    @TempDir
    Path rootDir;

    @Test
    void recentResultStaysInlineAgedResultBecomesSelfDescribingHandleWithRealReadback() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "hot-tail-e2e";
        PollingMockModel model = new PollingMockModel();
        ToolCallback bigTool = constantTool("fetch_log",
                "LOG-START 订单 ORD-7 全量日志 " + "数据行".repeat(600) + " LOG-END");

        // hot-tail(1)：仅最近 1 条工具结果全量内联；阈值 500 字符；即时 offload 被互斥关闭
        SpillModule spill = new SpillModule(rootDir, 128, 5);
        RuntimeConfig config = SpillGuardModule.fromModule(spill, rootDir)
                .thresholdChars(500)
                .hotTail(1)
                .build()
                .configure();

        AgentRuntime runtime = Buzhou.runtime(model, stores, config, bigTool);
        AgentSession session = runtime.spawn("spill-app", "log-agent", sid);

        session.chat("第一轮：拉日志"); // 调用 fetch_log → 唯一工具结果 = 最近 → 全量内联
        String firstView = model.lastToolResult == null ? "" : model.lastToolResult;
        assertThat(firstView).contains("LOG-START").contains("LOG-END"); // 近期零损失

        session.chat("第二轮：再拉一次"); // 2 条工具结果：最近内联、最早仍未过 keep-inline=1 边界？
        session.chat("第三轮：再拉一次"); // 3 条工具结果：前两条变旧 → 第一条被溢出为占位符
        session.close();

        // 第三轮注入视图（工具调用前）：最旧结果已被自描述占位符替换（句柄/形状/大小/回读动词）
        String agedView = model.preToolViews.size() >= 3
                ? model.preToolViews.get(2) : model.preToolViews.getLast();
        long spillHits = model.preToolViews.stream().filter(v -> v.contains("spill://")).count();
        assertThat(agedView)
                .as("旧结果应被自描述占位符替换；共 %d 个 pre-tool 视图、其中 %d 个含 spill 句柄；view=%s",
                        model.preToolViews.size(), spillHits, abbreviated(agedView))
                .contains("spill://" + io.github.chyuan_cuihongyuan.buzhou.spill.HotTailViewProcessor.VIEW_AGENT + "/")
                .contains("read_range");
        // 中间轮（第 2 轮 pre 视图）仅 1 条工具结果 = 最近 → 全量内联（hot-tail 语义本就如此）；
        // 第 3 轮 pre 视图有 2 条 → 最旧被溢出（上方断言）

        // 回读返回真实切片（经 read_range 工具，非编造）
        ReadRangeTool readRange = new ReadRangeTool(spill.service());
        String handle = extractHandle(agedView);
        assertThat(handle).isNotNull();
        String slice = readRange.call("{\"path\":\"" + handle + "\",\"mode\":\"bytes\",\"offset\":0,\"limit\":20}",
                null);
        assertThat(slice).contains("LOG-START");
    }

    private static String extractHandle(String view) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("spill://[\\w./-]+").matcher(view);
        return m.find() ? m.group() : null;
    }

    private static String abbreviated(String view) {
        return view.length() > 800 ? view.substring(0, 800) + "…(" + view.length() + " chars)" : view;
    }

    /** 轮询 mock：每轮调用 fetch_log；记录最近工具结果与各轮「见到工具结果前」的注入视图。 */
    static final class PollingMockModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();
        String lastToolResult;
        final List<String> preToolViews = new java.util.ArrayList<>();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            List<Message> msgs = prompt.getInstructions();
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last instanceof ToolResponseMessage toolResponse) {
                lastToolResult = toolResponse.getResponses().getFirst().responseData();
                return new ChatResponse(List.of(new Generation(new AssistantMessage("已记录日志"))));
            }
            preToolViews.add(msgs.toString());
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                            "tc-" + calls, "function", "fetch_log", "{}"))).build())));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static ToolCallback constantTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }
}
