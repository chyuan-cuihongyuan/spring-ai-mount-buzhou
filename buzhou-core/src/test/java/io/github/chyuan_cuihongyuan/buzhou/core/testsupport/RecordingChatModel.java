package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 录制装饰器（spec 12 §core-1 / impl-01）：包住真实（或替身）{@link ChatModel}，
 * 把每次调用的 (请求结构指纹, 响应脚本) 追加进 {@link RecordingFixture}，
 * 会话结束后 {@link #snapshot()}/{@link #writeTo(Path)} 落 JSON fixture，
 * 供 {@link FakeChatModel#fromRecording} 严格回放。
 *
 * <p>脱敏由 {@link RecordingFixture} 的序列化形状保证（只取结构指纹与响应脚本字段）。
 * 流式调用委托底层模型、不录制（回放由非流式脚本承担）。
 */
public final class RecordingChatModel implements TestDoubleChatModel {

    private final ChatModel delegate;
    private final List<RecordingFixture.Exchange> exchanges = new ArrayList<>();

    private RecordingChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    /** 包住一个模型开始录制。 */
    public static RecordingChatModel of(ChatModel delegate) {
        return new RecordingChatModel(delegate);
    }

    /** 目前已录制的交换序列（会话结束后取用）。 */
    public RecordingFixture snapshot() {
        return new RecordingFixture(List.copyOf(exchanges));
    }

    /** 已录制交换条数。 */
    public int recordedCount() {
        return exchanges.size();
    }

    /** 落 JSON fixture（默认约定 target/recordings/&lt;name&gt;.json）。 */
    public void writeTo(java.nio.file.Path path) {
        snapshot().writeTo(path);
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ChatResponse response = delegate.call(prompt);
        AssistantMessage assistant = response.getResult() == null
                ? new AssistantMessage("")
                : response.getResult().getOutput();
        exchanges.add(RecordingFixture.capture(prompt, assistant));
        return response;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(prompt);
    }
}
