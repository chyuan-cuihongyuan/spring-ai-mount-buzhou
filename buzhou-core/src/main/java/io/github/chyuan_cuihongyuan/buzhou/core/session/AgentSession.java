package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface AgentSession extends AutoCloseable {

    String sessionId();

    String appId();

    String agentName();

    String chat(String input);

    Flux<ChatResponse> stream(String input);

    void cancel();

    @Override
    void close();

    void addEventListener(SessionEventListener listener);

    void removeEventListener(SessionEventListener listener);
}
