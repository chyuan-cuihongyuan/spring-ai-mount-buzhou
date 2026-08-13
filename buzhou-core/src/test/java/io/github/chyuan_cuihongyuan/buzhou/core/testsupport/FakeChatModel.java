package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脚本化假模型（spec 12 §core-1 / impl-01）：按<b>调用序</b>弹出脚本步骤，多数场景下是
 * {@link ScriptedChatModel} 的强化替身——
 *
 * <ul>
 *   <li>脚本耗尽后<b>重复末步</b>（而非固定默认回复），多轮会话断言更稳；</li>
 *   <li>脚本步支持<b>单条 assistant 消息多个 toolCall</b>（{@link ScriptStep#parallel}，
 *       并行工具调用回放的关键语义）；</li>
 *   <li>{@link #fromRecording(RecordingFixture)} 从录制 fixture 构建<b>严格回放</b>：
 *       每次调用校验请求结构指纹（消息数 + 角色序列 + 在答工具名），<b>失配即
 *       {@link AssertionError}</b>；超出录制次数的调用同样失败（防静默漏断言）。</li>
 * </ul>
 *
 * <p>来源：Vercel AI SDK {@code MockLanguageModelV4}/{@code mockValues}（耗尽重复末值）+
 * Pydantic AI {@code TestModel/FunctionModel}（程序化构造工具调用）。Spring AI 官方无 fake
 * （仅 evaluation 模块），故自建并随 core test-jar 发布。
 */
public final class FakeChatModel implements TestDoubleChatModel {

    private final List<ScriptStep> steps;
    /** null = 宽松模式（耗尽重复末步）；非 null = 严格回放（校验指纹、禁止超录调用）。 */
    private final RecordingFixture fixture;
    private final AtomicInteger cursor = new AtomicInteger();

    /** 每次 {@link #call(Prompt)} 所见 prompt（供测试直接断言注入视图/回注内容）。 */
    public final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

    private FakeChatModel(List<ScriptStep> steps, RecordingFixture fixture) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("FakeChatModel 脚本不能为空");
        }
        this.steps = List.copyOf(steps);
        this.fixture = fixture;
    }

    /** 脚本化构建：按调用序消费。 */
    public static FakeChatModel script(ScriptStep... steps) {
        return new FakeChatModel(List.of(steps), null);
    }

    /** 脚本化构建（List 形式）。 */
    public static FakeChatModel script(List<ScriptStep> steps) {
        return new FakeChatModel(steps, null);
    }

    /**
     * 从录制 fixture 构建<b>严格回放</b>：响应脚本 = 录制响应；每次调用校验请求结构指纹，
     * 失配（会话漂移：消息数/角色序列/在答工具名不一致）即 {@link AssertionError}。
     */
    public static FakeChatModel fromRecording(RecordingFixture fixture) {
        List<ScriptStep> steps = fixture.exchanges().stream()
                .map(RecordingFixture.Exchange::toScriptStep)
                .toList();
        return new FakeChatModel(steps, fixture);
    }

    /** 已服务的模型调用次数。 */
    public int callCount() {
        return cursor.get();
    }

    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    @Override
    public org.springframework.ai.chat.model.ChatResponse call(Prompt prompt) {
        seenPrompts.add(prompt);
        int index = cursor.getAndIncrement();
        if (fixture != null) {
            List<RecordingFixture.Exchange> exchanges = fixture.exchanges();
            if (index >= exchanges.size()) {
                throw new AssertionError("回放超录：第 " + index + " 次模型调用超出录制的 "
                        + exchanges.size() + " 次交换——会话行为与录制漂移");
            }
            RecordingFixture.RequestFingerprint expected = exchanges.get(index).request();
            RecordingFixture.RequestFingerprint actual = RecordingFixture.RequestFingerprint.from(prompt);
            if (!expected.equals(actual)) {
                throw new AssertionError("回放失配（第 " + index + " 次调用）：录制指纹 "
                        + expected + " ≠ 实际指纹 " + actual + "——会话行为与录制漂移，拒绝静默回放");
            }
        }
        // 宽松模式耗尽后重复末步；严格回放模式上方的越界守卫已拦截超录调用
        ScriptStep step = index < steps.size() ? steps.get(index) : steps.getLast();
        return step.toChatResponse(index);
    }

    @Override
    public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }
}
