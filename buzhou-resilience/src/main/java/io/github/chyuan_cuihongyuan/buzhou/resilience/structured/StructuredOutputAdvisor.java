package io.github.chyuan_cuihongyuan.buzhou.resilience.structured;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 结构化输出执法 advisor（spec 402 / T695，instructor 借鉴——验证失败把
 * 错误喂回模型自修复）。
 *
 * <p>order = ToolCallingAdvisor.DEFAULT_ORDER + 480：精确缓存(+450)/语义
 * 缓存(+460)之内、观测(+500)/韧性(+700)之外——<b>每次修复重过观测+韧性层</b>
 * （真模型调用该被看见、该走重试/熔断）；外层缓存看到的键→已修复响应
 * （同请求得合法答案，缓存语义正确）。
 *
 * <p>流式（adviseStream）直通——聚合后无法重调，诚实边界（spec）。
 */
public class StructuredOutputAdvisor implements BaseAdvisor {

    /** 修复尝试事件（attempt + errors）。 */
    public static final String EVENT_REPAIR_ATTEMPTED = "structured-output.repair-attempted";
    /** 修复成功事件（attempt）。 */
    public static final String EVENT_REPAIRED = "structured-output.repaired";
    /** 修复耗尽违规事件（errors + attempts）。 */
    public static final String EVENT_VIOLATION = "structured-output.violation";

    static final int CHAIN_ORDER_OFFSET = 480;
    private static final int FEEDBACK_EXCERPT_LIMIT = 500;

    private final OutputSchema schema;
    private final int maxRepairAttempts;
    private final Consumer<SessionEvent> emitter;

    public StructuredOutputAdvisor(OutputSchema schema, int maxRepairAttempts,
            Consumer<SessionEvent> emitter) {
        this.schema = schema;
        this.maxRepairAttempts = Math.max(0, maxRepairAttempts);
        this.emitter = emitter == null ? e -> { } : emitter;
    }

    @Override
    public String getName() {
        return "BuzhouStructuredOutputAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    public OutputSchema schema() {
        return schema;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        // 首答走链（内层观测/hook/韧性全跑）；修复直达模型终端——链是单遍弹出
        // 式 Deque，重入 nextCall 必炸（ResilienceAdvisor 内层重试同口径约定：
        // 内层重试直达终端，外层把整环记一次逻辑调用；修复明细走本 advisor 事件）。
        ChatClientResponse response = callChain.nextCall(request);
        int attempt = 0;
        while (true) {
            String text = textOf(response);
            List<String> errors = schema.validate(text);
            if (errors.isEmpty()) {
                if (attempt > 0) {
                    emit(EVENT_REPAIRED, Map.of("attempt", attempt));
                }
                return response;
            }
            if (attempt >= maxRepairAttempts) {
                emit(EVENT_VIOLATION, Map.of("errors", String.join("; ", errors),
                        "attempts", attempt + 1));
                throw new StructuredOutputViolationException(errors);
            }
            attempt++;
            emit(EVENT_REPAIR_ATTEMPTED, Map.of("attempt", attempt,
                    "errors", String.join("; ", errors)));
            response = modelTerminal(callChain).adviseCall(rebuild(request, text, errors), callChain);
        }
    }

    /** 模型终端 = 链尾 advisor（ResilienceAdvisor.modelTerminal 同法）。 */
    private static org.springframework.ai.chat.client.advisor.api.CallAdvisor modelTerminal(
            CallAdvisorChain callChain) {
        List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> advisors =
                callChain.getCallAdvisors();
        org.springframework.ai.chat.client.advisor.api.CallAdvisor last =
                advisors.get(advisors.size() - 1);
        if (last instanceof StructuredOutputAdvisor) {
            throw new IllegalStateException(
                    "StructuredOutputAdvisor is innermost; no model-call terminal in chain");
        }
        return last;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
            StreamAdvisorChain streamChain) {
        return streamChain.nextStream(request); // 诚实边界：聚合后无法重调
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return request; // 执法在 adviseCall 内联（修复环需要链控制）
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return response;
    }

    /** 原指令 + 上轮 Assistant 回复 + 结构化反馈 UserMessage。 */
    private ChatClientRequest rebuild(ChatClientRequest request, String lastReply,
            List<String> errors) {
        List<org.springframework.ai.chat.messages.Message> messages =
                new ArrayList<>(request.prompt().getInstructions());
        messages.add(new AssistantMessage(lastReply == null ? "" : lastReply));
        messages.add(new UserMessage(feedback(errors, lastReply)));
        return request.mutate()
                .prompt(new Prompt(messages, request.prompt().getOptions()))
                .build();
    }

    private String feedback(List<String> errors, String lastReply) {
        String excerpt = lastReply == null ? "" : lastReply;
        if (excerpt.length() > FEEDBACK_EXCERPT_LIMIT) {
            excerpt = excerpt.substring(0, FEEDBACK_EXCERPT_LIMIT) + "…";
        }
        return "你上一条回复不符合要求的 JSON 输出契约。\n错误：\n- "
                + String.join("\n- ", errors)
                + "\n输出契约：" + schema.summary()
                + "\n请重新输出满足契约的单个 JSON 对象，不要附加解释或代码围栏。"
                + (excerpt.isEmpty() ? "" : "\n上一条回复（截断）：\n" + excerpt);
    }

    private static String textOf(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getResult() == null
                || response.chatResponse().getResult().getOutput() == null) {
            return "";
        }
        String text = response.chatResponse().getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private void emit(String type, Map<String, Object> payload) {
        emitter.accept(SessionEvent.of(type, new LinkedHashMap<>(payload)));
    }
}
