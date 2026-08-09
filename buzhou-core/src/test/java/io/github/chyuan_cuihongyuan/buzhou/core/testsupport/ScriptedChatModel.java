package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 脚本化 {@link ChatModel}（随 core test-jar 发布，供各模块/starter 装配测试复用）。
 *
 * <p>不触达真实模型：预入队若干 {@link AssistantMessage}，{@code call} 按序弹出并记录所见
 * {@link Prompt}；队列空时返回默认回复。用于 AutoConfig 装配后的会话级集成断言，
 * 避免装配测试依赖 API key / 网络。
 */
public class ScriptedChatModel implements ChatModel {

    public final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
    public final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

    public void enqueue(AssistantMessage message) {
        script.add(new ChatResponse(List.of(new Generation(message))));
    }

    public void enqueueText(String content) {
        enqueue(new AssistantMessage(content));
    }

    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        seenPrompts.add(prompt);
        ChatResponse next = script.poll();
        if (next == null) {
            next = new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }
        return next;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    /** 工具响应消息是否包含给定文本（装配集成断言辅助）。 */
    public static boolean contains(Message m, String text) {
        if (m instanceof ToolResponseMessage trm) {
            return trm.getResponses().stream()
                    .anyMatch(r -> r.responseData() != null && r.responseData().contains(text));
        }
        return m.getText() != null && m.getText().contains(text);
    }
}
