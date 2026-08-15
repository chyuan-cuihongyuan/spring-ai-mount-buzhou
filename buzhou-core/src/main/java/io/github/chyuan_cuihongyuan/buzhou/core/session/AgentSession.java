package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface AgentSession extends AutoCloseable {

    String sessionId();

    String appId();

    String agentName();

    String chat(String input);

    /**
     * 多模态输入（spec 27 / T106 / impl-81）：文本 + 媒体引用（图片/PDF 等，URI 形态）。
     * 媒体仅随最近一条带媒体的用户消息重发，更早轮次降级为文本标记（token 成本口径
     * 见 {@link MediaRef#TOKENS_PER_MEDIA}）。默认不支持（显式失败，不静默丢媒体）。
     */
    default String chat(String input, java.util.List<MediaRef> media) {
        throw new UnsupportedOperationException("本 AgentSession 实现不支持多模态输入");
    }

    /**
     * 结构化输出（spec 19 / T87 / impl-62）：要求模型按 {@code type} 的 JSON schema 输出并解析为
     * 实例。解析失败自动 REASK 一次（携带解析错误反馈的完整轮次，诚实计入预算）；再失败抛
     * {@link StructuredOutputException}。流式不支持（JSON 增量解析语义不明）。
     */
    default <T> T chatForEntity(String input, Class<T> type) {
        throw new UnsupportedOperationException(
                "本 AgentSession 实现不支持结构化输出（chatForEntity）");
    }

    /** 结构化输出 + 多模态输入（spec 27：媒体随两次尝试全程携带）。 */
    default <T> T chatForEntity(String input, java.util.List<MediaRef> media, Class<T> type) {
        throw new UnsupportedOperationException(
                "本 AgentSession 实现不支持结构化输出（chatForEntity）");
    }

    Flux<ChatResponse> stream(String input);

    /** 流式 + 多模态输入（spec 27；媒体语义同 {@link #chat(String, java.util.List)}）。 */
    default Flux<ChatResponse> stream(String input, java.util.List<MediaRef> media) {
        throw new UnsupportedOperationException("本 AgentSession 实现不支持多模态输入");
    }

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

    /**
     * impl-35 / spec 13 §stores-6：删除会话 = 一次调用清干净——先 {@link #close()}
     * （资源注册表清空：spill 文件 / 租约释放 / executor 排空），再经 SessionCleaner
     * 级联删除全部存储（messages / summaries / state / lease / spans / events /
     * snapshots / tool_call_log / run_registry + 配置挂接的贡献者）。清理失败逐目标隔离、
     * 聚合上抛（首个失败 + suppressed，与 impl-30 close 语义对齐）；对已 close 的会话仍执行
     * 存储级联（close 幂等）。未接线 SessionCleaner 的实现退化为 close()。
     */
    default void delete() {
        close();
    }

    void addEventListener(SessionEventListener listener);

    void removeEventListener(SessionEventListener listener);

    /**
     * impl-34 / spec 13 §core-4：事件总线运行时统计（overflow 丢弃可见）。仅
     * {@code buffered} 分发模式适用（有队列、有丢弃）；SYNC 内联分发（默认）返回空。
     */
    default java.util.Optional<EventBusStats> eventBusStats() {
        return java.util.Optional.empty();
    }
}
