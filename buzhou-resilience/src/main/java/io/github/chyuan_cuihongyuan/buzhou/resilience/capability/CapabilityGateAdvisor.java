package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

/**
 * 能力门 advisor（spec 502 / T756，LiteLLM Router capabilities 借鉴）：
 * 按请求需求（media→vision、工具回调→tools）对照当前模型注册能力
 * <b>事前</b>拦截——供应商 400（事后错）变结构化 BuzhouException。
 *
 * <p>order = ToolCallingAdvisor.DEFAULT_ORDER + 620：rate-limit(+650) 内、
 * 模型调用前。拒绝在 nextCall 前抛 → ResilienceAdvisor 不可见（不进重试
 * 分类——换模型可解重试同模型无益，NON_RETRYABLE）→ HookAdvisor.
 * onModelError 可兜底（并发舱/限流同语义）。
 *
 * <p>诚实边界：单模型名口径（buzhou.model-name）；未注册模型零门零行为
 * （声明渐进——不声明不误拦）。
 */
public final class CapabilityGateAdvisor implements BaseAdvisor {

    /** advisor 链 order 偏移：capability-gate=+620（rate-limit+650 内）。 */
    static final int CHAIN_ORDER_OFFSET = 620;

    private final ModelCapabilityRegistry registry;
    private final String modelName;

    public CapabilityGateAdvisor(ModelCapabilityRegistry registry, String modelName) {
        this.registry = registry;
        this.modelName = modelName == null ? "unknown" : modelName;
    }

    @Override
    public String getName() {
        return "BuzhouCapabilityGateAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    /** BaseAdvisor 抽象槽：请求/响应原样（门逻辑全部在 advise* 的前置检查）。 */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    /** BaseAdvisor 抽象槽：响应原样（同上）。 */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        gate(request);
        return callChain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        gate(request);
        return streamChain.nextStream(request);
    }

    /** 未注册模型零门；注册且缺能力 → ARGS_VALIDATION_FAILED（NON_RETRYABLE）。 */
    private void gate(ChatClientRequest request) {
        ModelCapabilities capabilities = registry.of(modelName);
        if (capabilities == null) {
            return; // 未注册 = 零门透传（声明渐进、不声明不误拦）
        }
        if (requiresVision(request) && !capabilities.vision()) {
            throw new BuzhouException(ErrorCode.ARGS_VALIDATION_FAILED,
                    "模型 " + modelName + " 未声明 vision 能力（buzhou.resilience."
                            + "model-capabilities." + modelName + ".vision=false）——"
                            + "多模态请求被能力门事前拦截，请路由到具备视觉能力的模型");
        }
        if (requiresTools(request) && !capabilities.tools()) {
            throw new BuzhouException(ErrorCode.ARGS_VALIDATION_FAILED,
                    "模型 " + modelName + " 未声明 tools 能力（buzhou.resilience."
                            + "model-capabilities." + modelName + ".tools=false）——"
                            + "带工具请求被能力门事前拦截，请路由到支持 function calling 的模型");
        }
    }

    /** 任一用户消息携带 media = 需 vision（公开 API 检测，零反射）。 */
    private static boolean requiresVision(ChatClientRequest request) {
        return request.prompt().getInstructions().stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .anyMatch(message -> message.getMedia() != null && !message.getMedia().isEmpty());
    }

    /** 工具回调非空 = 需 tools。 */
    private static boolean requiresTools(ChatClientRequest request) {
        ChatOptions options = request.prompt().getOptions();
        return options instanceof ToolCallingChatOptions toolOptions
                && toolOptions.getToolCallbacks() != null
                && !toolOptions.getToolCallbacks().isEmpty();
    }
}
