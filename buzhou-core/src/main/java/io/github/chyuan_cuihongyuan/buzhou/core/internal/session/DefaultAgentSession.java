package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultSessionEventContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
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
    private final HookChain hookChain;
    private final HookEnvironment hookEnv;
    private final io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager toolManager;
    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager toolManager) {
        this.appId = appId;
        this.agentName = agentName;
        this.sessionId = sessionId;
        this.chatClient = chatClient;
        this.registry = registry;
        this.onClose = onClose;
        this.hookChain = hookChain;
        this.hookEnv = hookEnv;
        this.toolManager = toolManager;
        this.hookEnv.bindEventPublisher(this::dispatchEvent);
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
        hookEnv.nextTurn();
        DefaultTurnContext turnCtx = new DefaultTurnContext(hookEnv, input);
        HookResult before = hookChain.beforeTurn(turnCtx);
        if (before instanceof HookResult.Block block) {
            return block.reason();
        }
        String response = chatClient.prompt()
                .user(turnCtx.input())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
        turnCtx.markResponded(response);
        hookChain.afterTurn(turnCtx);
        return turnCtx.response();
    }

    @Override
    public Flux<ChatResponse> stream(String input) {
        ensureOpen();
        hookEnv.nextTurn();
        DefaultTurnContext turnCtx = new DefaultTurnContext(hookEnv, input);
        HookResult before = hookChain.beforeTurn(turnCtx);
        if (before instanceof HookResult.Block block) {
            return Flux.error(new IllegalStateException(block.reason()));
        }
        return chatClient.prompt()
                .user(turnCtx.input())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse();
    }

    /** 取消在途轮次：中断全部在途工具调用；会话不谢幕，可继续 chat。 */
    @Override
    public void cancel() {
        ensureOpen();
        toolManager.cancelInFlight();
        dispatchEvent(SessionEvent.of("session.cancelled"));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            onClose.run();
            dispatchEvent(SessionEvent.of("session.closed"));
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

    private void dispatchEvent(SessionEvent event) {
        hookChain.fireEvent(new DefaultSessionEventContext(hookEnv, event));
        listeners.forEach(listener -> listener.onEvent(event));
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Session already closed: " + sessionId);
        }
    }
}
