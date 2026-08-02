package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultAgentSession implements AgentSession {

    private final String appId;
    private final String agentName;
    private final String sessionId;
    private final ChatClient chatClient;
    private final SessionResourceRegistry registry;
    private final Runnable onClose;
    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose) {
        this.appId = appId;
        this.agentName = agentName;
        this.sessionId = sessionId;
        this.chatClient = chatClient;
        this.registry = registry;
        this.onClose = onClose;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String appId() {
        return appId;
    }

    @Override
    public String agentName() {
        return agentName;
    }

    @Override
    public String chat(String input) {
        ensureOpen();
        return chatClient.prompt()
                .user(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    @Override
    public Flux<ChatResponse> stream(String input) {
        ensureOpen();
        return chatClient.prompt()
                .user(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse();
    }

    /** 取消在途轮次。执行脊柱（并行工具调用）随 ticket 09 落地后接入取消传播；当前为事件占位。 */
    @Override
    public void cancel() {
        ensureOpen();
        fire(SessionEvent.of("session.cancelled"));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            onClose.run();
            fire(SessionEvent.of("session.closed"));
            listeners.clear();
        }
    }

    @Override
    public void addEventListener(SessionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(SessionEventListener listener) {
        listeners.remove(listener);
    }

    private void fire(SessionEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Session already closed: " + sessionId);
        }
    }

}
