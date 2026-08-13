package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可运行 demo（impl/05 / T4）——一条命令眼见 Buzhou core 跑起来：多轮 + 工具 + 渐进式压缩。
 *
 * <p>纯编程式装配（spec「方式三 纯编程式」）：{@code Buzhou.runtime(...)} + {@link MemoryModule}，
 * 无需 Spring Boot、无需 API key——用确定性 {@link StubChatModel}（可替换为真实模型，见 {@link #run}）。
 * 预置一段「排障 Agent」多轮历史（每轮含大工具返回），再跑一轮对话：旧轮大工具返回被
 * <b>微压缩</b>为 evidence 占位符，模型仍可经 read_evidence 工具回查原文——core 端到端可用即此证明。
 *
 * <p>同一份 {@link #run(StubChatModel)} 逻辑被 {@code main}（人看）与 {@code BuzhouDemoTest}（机器守）共用。
 *
 * <p>运行：{@code mvn -q -pl examples -am compile && java -cp <examples+deps> ...BuzhouDemo}
 * （或 IDE 直接运行 {@link #main}）。
 */
public final class BuzhouDemo {

    /** 预埋事实：微压缩后须仍可经 evidence 回查（P0 锚定语义）。 */
    static final String ORDER_ID = "ORD-2026-7";
    static final String ERROR_CODE = "ERR_TIMEOUT_5421";

    private BuzhouDemo() {
    }

    public static void main(String[] args) {
        DemoResult result = run(new StubChatModel("好的，我继续排查——已回顾历史，网关层超时仍是最可能根因。"));
        System.out.println(result.transcript());
    }

    /**
     * 跑一遍 demo 并返回可观察结果。
     *
     * @param liveModel 本轮对话用的模型（demo 默认传 {@link StubChatModel}；可替换为真实
     *                  {@link ChatModel}——但真实模型不暴露注入视图，故 compaction 可见性仅 stub 场景可断言，
     *                  真实场景靠模型行为体现）。
     */
    public static DemoResult run(StubChatModel liveModel) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "demo-session";
        int turns = 10;
        // 预置排障历史：每轮 user 提问（大详情）→ assistant 调 get_order_status → 大工具返回 → assistant 小结。
        // 规模取与既有 demo 测试一致（足以在默认窗口触发微压缩）。
        stores.messageStore().append(sid, seedHistory(sid, turns));

        // 微压缩配置（默认窗口、无摘要模型）：旧轮大工具返回会被替换为 evidence 占位符
        RuntimeConfig config = MemoryModule.configure(Map.of(), stores.messageStore());
        AgentRuntime runtime = Buzhou.runtime(liveModel, stores, config);

        AgentSession session = runtime.spawn("demo-app", "support-agent", sid);
        String reply = session.chat("继续排查");
        session.close();

        // 本轮注入视图 = liveModel 首个 seenPrompt 的指令（微压缩在此发生）
        String view = liveModel.firstInstructions();
        boolean compacted = view.contains("旧工具结果已清理") && view.contains("evidence-id=");
        String evidenceId = extractEvidenceId(view);
        String evidenceOriginal = evidenceId == null ? null
                : new EvidenceLookupTool(stores.messageStore())
                        .call("{\"evidenceId\":\"" + evidenceId + "\"}");

        String transcript = buildTranscript(turns, reply, compacted, evidenceId, evidenceOriginal, view);
        return new DemoResult(transcript, reply, compacted, evidenceId, evidenceOriginal);
    }

    /** 构造排障历史（每轮含大工具返回，触发微压缩）。 */
    static List<BuzhouMessage> seedHistory(String sessionId, int turns) {
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= turns; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER,
                    "排查订单 " + ORDER_ID + " 第" + turn + "步：" + "详情字段".repeat(120),
                    List.of(), null));
            history.add(msg(sessionId, turn, 1, Role.ASSISTANT, "",
                    List.of(new ToolCallRecord("tc-" + turn, "get_order_status", "{}")), null));
            history.add(msg(sessionId, turn, 2, Role.TOOL,
                    "[日志] 订单 " + ORDER_ID + " 错误码 " + ERROR_CODE + " " + "查询行数据".repeat(160),
                    List.of(), "tc-" + turn));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT,
                    "第" + turn + "步结论：网关层超时", List.of(), null));
        }
        return history;
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role,
                                     String content, List<ToolCallRecord> toolCalls, String toolCallId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role, content,
                toolCalls, toolCallId, null, null, Map.of(), Instant.now());
    }

    private static String buildTranscript(int turns, String reply, boolean compacted,
                                          String evidenceId, String evidenceOriginal, String view) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══ Buzhou 可运行 demo：多轮 + 工具 + 渐进式记忆压缩 ═══\n");
        sb.append("场景：排障 Agent（订单 ").append(ORDER_ID).append("），预置 ").append(turns)
                .append(" 轮历史，每轮调 get_order_status 拿大日志。\n");
        sb.append("装配：Buzhou.runtime(stubModel, inMemoryStores, MemoryModule) —— 纯编程式、无 API key。\n");
        sb.append("──────────────────────────────────────────────────────\n");
        sb.append("[本轮] 用户: 继续排查\n");
        sb.append("[本轮] 模型: ").append(reply).append("\n");
        sb.append("──────────────────────────────────────────────────────\n");
        sb.append("[微压缩] 旧轮大工具返回已压缩为 evidence 占位符: ").append(compacted).append("\n");
        if (evidenceId != null) {
            sb.append("[回查]   evidence-id=").append(evidenceId).append("\n");
            sb.append("[回查]   read_evidence 取回原文含订单号 ").append(ORDER_ID).append(": ")
                    .append(evidenceOriginal != null && evidenceOriginal.contains(ORDER_ID)).append("\n");
        }
        sb.append("──────────────────────────────────────────────────────\n");
        sb.append("（注入视图片段，展示压缩发生）\n");
        sb.append(excerpt(view, "旧工具结果已清理"));
        sb.append("\n═══ core 端到端跑通：多轮历史 + 工具调用 + 微压缩 + evidence 回查 ═══\n");
        return sb.toString();
    }

    /** 取注入视图中首个命中锚点附近的一段（demo 可读性，避免打印整个视图）。 */
    private static String excerpt(String view, String anchor) {
        int i = view.indexOf(anchor);
        if (i < 0) {
            return "(未找到 " + anchor + " —— 压缩可能未触发)";
        }
        int from = Math.max(0, i - 40);
        int to = Math.min(view.length(), i + 120);
        return "..." + view.substring(from, to).replace("\n", " ") + "...";
    }

    private static String extractEvidenceId(String view) {
        Matcher m = Pattern.compile("evidence-id=([\\w-]+)").matcher(view);
        return m.find() ? m.group(1) : null;
    }

    /** demo 运行结果（main 打印、test 断言共用）。 */
    public record DemoResult(String transcript, String reply, boolean compacted,
                             String evidenceId, String evidenceOriginal) {
    }

    /**
     * 确定性 stub {@link ChatModel}：固定回复 + 记录所见 Prompt（供 demo 断言注入视图）。
     * 不触达真实模型——demo 无需 API key 即可跑、CI 可断言。真实模型接入见类 javadoc。
     */
    public static final class StubChatModel implements ChatModel {
        private final String reply;
        final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

        public StubChatModel(String reply) {
            this.reply = reply;
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        String firstInstructions() {
            return seenPrompts.isEmpty() ? "" : seenPrompts.get(0).getInstructions().toString();
        }
    }
}
