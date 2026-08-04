package io.github.chyuan_cuihongyuan.buzhou.observability.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.BaseSpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.observability.thinking.ExtractedThinking;
import io.github.chyuan_cuihongyuan.buzhou.observability.thinking.ThinkingChainExtractor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可观测 advisor（spec 03 挂接点）。
 *
 * <p>order = {@code ToolCallingAdvisor.DEFAULT_ORDER + 500}，介于 memory(+400) 与 hook(+600)：
 * 晚于 memory 见注入视图（便于落 {@link InjectionSnapshot}）、先于 hook 见响应。
 *
 * <p>职责：开/关 Turn span 与 ModelCall span（每次模型迭代）、采集 usage / finish_reason / 思维链 /
 * 最终回复、构建并落库每轮注入快照。SpanContext 经 {@link io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier}
 * 显式下传到 ToolCallback 包装层。
 */
public class ObservabilityAdvisor implements BaseAdvisor {

    private final BaseSpanRecorder recorder;
    private final ObservabilityConfig config;
    private final ThinkingChainExtractor thinkingExtractor;
    private final TokenEstimator tokenEstimator;
    private final ObservabilitySessionHooks hooks;
    private final String modelName;
    private final MicrometerDualWriter meters;

    public ObservabilityAdvisor(BaseSpanRecorder recorder, ObservabilityConfig config,
                                ThinkingChainExtractor thinkingExtractor, TokenEstimator tokenEstimator,
                                ObservabilitySessionHooks hooks, String modelName) {
        this.recorder = recorder;
        this.config = config;
        this.thinkingExtractor = thinkingExtractor;
        this.tokenEstimator = tokenEstimator;
        this.hooks = hooks;
        this.modelName = modelName;
        this.meters = recorder.meters();
    }

    @Override
    public String getName() {
        return "BuzhouObservabilityAdvisor";
    }

    @Override
    public int getOrder() {
        // spec 03 挂接点：循环内 advisor order = ToolCallingAdvisor.DEFAULT_ORDER + 400。
        // 实际实现中 BuzhouMemoryAdvisor 用 +400，故此处用 +500（+400 冲突时本 advisor 需不同值）。
        // 语义不变：在 memory(+400) 之后、hook(+600) 之前。
        return ToolCallingAdvisor.DEFAULT_ORDER + 500;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        int turnSeq = currentTurnSeq(request);
        // MODEL_CALL 的 parent 是所属 TURN span（spec 03 时序图）；TURN 未开时兜底挂 SESSION
        SpanContext turnParent = resolveTurnParent(turnSeq);
        // 注入快照：memory advisor 已重建注入视图（+400 < +500），此处捕获
        captureInjectionSnapshot(request, turnSeq);
        SpanHandle modelCall = openModelCall(turnParent, request);
        ChatClientResponse response;
        try {
            response = callChain.nextCall(request);
        } catch (RuntimeException e) {
            modelCall.error(e);
            modelCall.close();
            throw e;
        }
        recordModelCallOutcome(modelCall, response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        int turnSeq = currentTurnSeq(request);
        captureInjectionSnapshot(request, turnSeq);
        SpanContext parent = resolveTurnParent(turnSeq);
        SpanHandle modelCall = openModelCall(parent, request);
        // 流式增量在 span 关闭时聚合（思维链 delta 合并单事件，spec 推演 2；正文同理聚合 FINAL_REPLY）
        StringBuilder thinkingAccumulator = new StringBuilder();
        StringBuilder replyAccumulator = new StringBuilder();
        AtomicReference<Usage> lastUsage = new AtomicReference<>();
        AtomicReference<String> lastFinishReason = new AtomicReference<>();
        AtomicReference<Boolean> sawToolCalls = new AtomicReference<>(false);
        return streamChain.nextStream(request)
                .doOnEach(signal -> {
                    ChatClientResponse resp = signal.get();
                    if (resp != null && resp.chatResponse() != null) {
                        accumulateStreamChunk(resp.chatResponse(), thinkingAccumulator, replyAccumulator,
                                lastUsage, lastFinishReason, sawToolCalls);
                    }
                })
                .doOnError(e -> {
                    modelCall.error(e);
                    modelCall.close();
                })
                .doOnComplete(() -> {
                    recordStreamOutcome(modelCall, thinkingAccumulator, replyAccumulator,
                            lastUsage.get(), lastFinishReason.get(), sawToolCalls.get());
                });
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    private SpanHandle openModelCall(SpanContext parent, ChatClientRequest request) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("model.name", modelName);
        attrs.put("iteration", nextIteration(parent));
        return recorder.openSpan(SpanKind.MODEL_CALL, "model-call", parent, attrs);
    }

    /** MODEL_CALL 的 parent：优先所属 TURN span（spec 03 时序图），TURN 未开时兜底挂 SESSION。 */
    private SpanContext resolveTurnParent(int turnSeq) {
        SpanContext turnSpan = hooks.turnSpan();
        if (turnSpan != null) {
            return turnSpan;
        }
        SpanContext sessionSpan = hooks.sessionSpan();
        return sessionSpan != null
                ? new SpanContext(sessionSpan.spanId(), sessionSpan.sessionId(), turnSeq) : null;
    }

    private void recordStreamOutcome(SpanHandle modelCall, StringBuilder thinkingAccumulator,
                                     StringBuilder replyAccumulator, Usage usage,
                                     String finishReason, boolean sawToolCalls) {
        if (usage != null) {
            if (usage.getPromptTokens() != null) {
                modelCall.attribute("usage.prompt_tokens", usage.getPromptTokens());
                meters.recordTokens(modelName, "prompt", usage.getPromptTokens());
            }
            if (usage.getCompletionTokens() != null) {
                modelCall.attribute("usage.completion_tokens", usage.getCompletionTokens());
                meters.recordTokens(modelName, "completion", usage.getCompletionTokens());
            }
            if (hooks instanceof ObservabilitySessionState state) {
                state.accumulateTurnUsage(usage.getPromptTokens(), usage.getCompletionTokens());
            }
        }
        if (finishReason != null) {
            modelCall.attribute("finish_reason", finishReason);
        }
        if (thinkingAccumulator.length() > 0) {
            recorder.emit(modelCall.context(), EventType.THINKING,
                    Map.of("content", thinkingAccumulator.toString(),
                            "provider.key", "stream-accumulated"));
            modelCall.attribute("thinking.available", "YES");
        }
        if (!sawToolCalls && replyAccumulator.length() > 0) {
            Map<String, Object> reply = new LinkedHashMap<>();
            reply.put("content", replyAccumulator.toString());
            if (finishReason != null) {
                reply.put("finish_reason", finishReason);
            }
            recorder.emit(modelCall.context(), EventType.FINAL_REPLY, reply);
        }
        modelCall.close();
    }

    private void accumulateStreamChunk(ChatResponse chatResponse, StringBuilder thinkingAccumulator,
                                       StringBuilder replyAccumulator, AtomicReference<Usage> lastUsage,
                                       AtomicReference<String> lastFinishReason,
                                       AtomicReference<Boolean> sawToolCalls) {
        if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
            Usage u = chatResponse.getMetadata().getUsage();
            if (u.getPromptTokens() != null || u.getCompletionTokens() != null) {
                lastUsage.set(u);
            }
        }
        Generation result = chatResponse.getResult();
        AssistantMessage assistant = result == null ? null : result.getOutput();
        if (assistant == null) {
            return;
        }
        if (result.getMetadata() != null && result.getMetadata().getFinishReason() != null
                && !result.getMetadata().getFinishReason().isBlank()) {
            lastFinishReason.set(result.getMetadata().getFinishReason());
        }
        if (assistant.hasToolCalls()) {
            sawToolCalls.set(true);
        }
        if (assistant.getText() != null) {
            replyAccumulator.append(assistant.getText());
        }
        thinkingExtractor.extract(assistant).ifPresent(t -> {
            if (t.content() != null && !t.content().isBlank()) {
                thinkingAccumulator.append(t.content());
            }
        });
    }

    private void recordModelCallOutcome(SpanHandle modelCall, ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            modelCall.close();
            return;
        }
        ChatResponse chatResponse = response.chatResponse();
        Usage usage = chatResponse.getMetadata() == null ? null : chatResponse.getMetadata().getUsage();
        if (usage != null) {
            if (usage.getPromptTokens() != null) {
                modelCall.attribute("usage.prompt_tokens", usage.getPromptTokens());
                meters.recordTokens(modelName, "prompt", usage.getPromptTokens());
            }
            if (usage.getCompletionTokens() != null) {
                modelCall.attribute("usage.completion_tokens", usage.getCompletionTokens());
                meters.recordTokens(modelName, "completion", usage.getCompletionTokens());
            }
            // 把本轮 usage 累加给会话状态（onTurnEnd 聚合到 TURN span）
            if (hooks instanceof ObservabilitySessionState state) {
                state.accumulateTurnUsage(usage.getPromptTokens(), usage.getCompletionTokens());
            }
        }
        Generation result = chatResponse.getResult();
        AssistantMessage assistant = result == null ? null : result.getOutput();
        String finishReason = result != null && result.getMetadata() != null
                ? result.getMetadata().getFinishReason() : null;
        if (finishReason != null) {
            modelCall.attribute("finish_reason", finishReason);
        }
        if (assistant != null) {
            // 思维链
            Optional<ExtractedThinking> thinking = config.thinkingCapture()
                    ? thinkingExtractor.extract(assistant) : Optional.empty();
            if (thinking.isPresent()) {
                ExtractedThinking t = thinking.get();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("content", t.content());
                payload.put("provider.key", t.providerKey());
                if (t.signature() != null) {
                    payload.put("signature", t.signature());
                }
                payload.put("omitted", t.omitted());
                if (t.truncated()) {
                    payload.put("truncated", true);
                    payload.put("original.length", t.originalLength());
                }
                recorder.emit(modelCall.context(), EventType.THINKING, payload);
                modelCall.attribute("thinking.available", "YES");
            } else {
                // 官方 OpenAI（GPT-5/o1/o3）：无推理文本，仅 usage reasoning_tokens —— 固定降级路径
                // 其他厂商 key 缺失属正常（未开启 thinking 或模型本身无思维链）
                boolean isOfficialOpenAi = modelName != null
                        && (modelName.contains("gpt") || modelName.contains("o1") || modelName.contains("o3"));
                if (isOfficialOpenAi) {
                    modelCall.attribute("thinking.available", "PROVIDER_NOT_RETURNED");
                } else {
                    modelCall.attribute("thinking.available", "NO");
                }
            }
            // 最终回复（无 tool_calls 即完结）
            if (!assistant.hasToolCalls() && assistant.getText() != null && !assistant.getText().isBlank()) {
                Map<String, Object> reply = new LinkedHashMap<>();
                reply.put("content", assistant.getText());
                if (finishReason != null) {
                    reply.put("finish_reason", finishReason);
                }
                recorder.emit(modelCall.context(), EventType.FINAL_REPLY, reply);
            }
        }
        modelCall.close();
    }

    private void captureInjectionSnapshot(ChatClientRequest request, int turnSeq) {
        if (!config.snapshotCapture()) {
            return;
        }
        SpanContext sessionSpan = hooks.sessionSpan();
        String sessionId = sessionSpan != null ? sessionSpan.sessionId() : null;
        if (sessionId == null) {
            return;
        }
        List<Message> instructions = request.prompt().getInstructions();
        List<String> messageIds = new ArrayList<>();
        List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SnapshotMessage> messages = new ArrayList<>();
        int systemTokens = 0;
        int userTokens = 0;
        int assistantTokens = 0;
        int idx = 0;
        for (Message m : instructions) {
            String role = m.getMessageType().name();
            messageIds.add(role + ":" + (idx++));
            String content = m.getText();
            int tokens = tokenEstimator.estimate(content);
            switch (m.getMessageType()) {
                case SYSTEM -> systemTokens += tokens;
                case USER -> userTokens += tokens;
                case ASSISTANT -> assistantTokens += tokens;
                default -> {
                }
            }
            // 检查是否为占位符/引用句柄（evidence-id / spill URI 从 metadata 提取）
            String evidenceId = null;
            String spillUri = null;
            if (m instanceof org.springframework.ai.chat.messages.ToolResponseMessage trm) {
                // ToolResponseMessage 是工具返回，可能已被微压缩为占位符
                // 占位符格式：[spill:xxx] 或含 evidence-id；这里简化：正文即可能含占位符
            }
            messages.add(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SnapshotMessage(
                    role, content, evidenceId, spillUri, Map.of()));
        }
        Map<String, Object> budget = new LinkedHashMap<>();
        budget.put("messages.count", instructions.size());
        budget.put("tokens.system", systemTokens);
        budget.put("tokens.user", userTokens);
        budget.put("tokens.assistant", assistantTokens);
        budget.put("tokens.total", systemTokens + userTokens + assistantTokens);
        InjectionSnapshot snapshot = new InjectionSnapshot(sessionId, turnSeq, messageIds, messages, budget,
                "", Instant.now());
        recorder.enqueue(new io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingSnapshot(snapshot));
    }

    private int currentTurnSeq(ChatClientRequest request) {
        Integer seq = hooks.currentTurnSeq(conversationId(request));
        return seq == null ? 0 : seq;
    }

    private int nextIteration(SpanContext parent) {
        return hooks.nextIteration(parent == null ? null : parent.sessionId());
    }

    private static String conversationId(ChatClientRequest request) {
        Object value = request.context().get(ChatMemory.CONVERSATION_ID);
        return value instanceof String s ? s : null;
    }

    /** 按 session 暴露 turn/iteration 跟踪与会话 span，供 advisor 与 observable 工具回调共享。 */
    public interface ObservabilitySessionHooks {
        SpanContext sessionSpan();

        /** 当前 TURN span 的 SpanContext（MODEL_CALL/TOOL_CALL 的 parent）；未开轮时返回 null。 */
        SpanContext turnSpan();

        Integer currentTurnSeq(String sessionId);

        int nextIteration(String sessionId);
    }
}
