package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface AgentSession extends AutoCloseable {

    String sessionId();

    String appId();

    String agentName();

    String chat(String input);

    Flux<ChatResponse> stream(String input);

    /** 取消在途轮次（impl-05）：立即中断在飞工具、丢弃在飞结果；会话不谢幕，可继续 chat。 */
    void cancel();

    /**
     * 按取消模式取消在途轮次（impl-05 / T31）：
     * {@link CancelMode#IMMEDIATE} 立即中断；{@link CancelMode#AFTER_CURRENT_TOOLS}
     * 当前工具批完成后停止递归；{@link CancelMode#AFTER_CURRENT_TURN} 本轮完整收尾（仅标记）。
     */
    void cancel(CancelMode mode);

    @Override
    void close();

    void addEventListener(SessionEventListener listener);

    void removeEventListener(SessionEventListener listener);
}
