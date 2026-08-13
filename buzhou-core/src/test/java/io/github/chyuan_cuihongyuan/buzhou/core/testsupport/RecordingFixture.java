package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 录制 fixture（JSON）：一次真实（或替身）会话的 (请求结构指纹, 响应脚本) 序列。
 *
 * <p><b>脱敏由构造保证</b>：只序列化结构指纹（消息数/角色序列/在答工具名）与响应脚本
 * （文本 + 工具名 + JSON 入参）——不落 headers、options 原文、凭据或环境值。
 *
 * <p>约定：录制默认写 {@code target/recordings/<name>.json}（不提交）；精选 fixture 手工拷入
 * {@code src/test/resources/recordings/} 随仓发布，经 {@link #loadClasspath(String)} 回放。
 *
 * @param exchanges 录制的交换序列
 */
public record RecordingFixture(
        @JsonProperty("exchanges") List<Exchange> exchanges) {

    /** 单次交换：请求结构指纹 + 响应脚本。 */
    public record Exchange(
            @JsonProperty("request") RequestFingerprint request,
            @JsonProperty("response") ResponseSpec response) {

        ScriptStep toScriptStep() {
            List<ToolCallSpec> calls = response.toolCalls().stream()
                    .map(c -> new ToolCallSpec(c.name(), c.arguments()))
                    .toList();
            return new ScriptStep(response.text() == null ? "" : response.text(), calls);
        }
    }

    /** 响应脚本：assistant 文本 + 工具调用（结构化字段）。 */
    public record ResponseSpec(
            @JsonProperty("text") String text,
            @JsonProperty("toolCalls") List<ToolCallSpecJson> toolCalls) {
    }

    /** JSON 持久化形状的工具调用。 */
    public record ToolCallSpecJson(
            @JsonProperty("name") String name,
            @JsonProperty("arguments") String arguments) {
    }

    /**
     * 请求结构指纹：消息数 + 角色序列 + 在答工具名（末消息为 ToolResponseMessage 时）。
     * <b>只取结构不取内容</b>——既能抓住会话漂移（轮次错序/工具错配），又不因正文里的
     * 会话 id / 时间戳等噪声而误报。
     */
    public record RequestFingerprint(
            @JsonProperty("messageCount") int messageCount,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("respondedToolCalls") List<String> respondedToolCalls) {

        static RequestFingerprint from(Prompt prompt) {
            List<Message> messages = prompt.getInstructions();
            List<String> roles = messages.stream()
                    .map(m -> m.getMessageType().name())
                    .toList();
            List<String> responded = messages.isEmpty()
                    || !(messages.getLast() instanceof ToolResponseMessage toolResponse)
                    ? List.of()
                    : toolResponse.getResponses().stream()
                            .map(ToolResponseMessage.ToolResponse::name)
                            .filter(Objects::nonNull)
                            .toList();
            return new RequestFingerprint(messages.size(), roles, responded);
        }
    }

    /** 从真实/替身会话采集交换（RecordingChatModel 内部使用）。 */
    static Exchange capture(Prompt prompt, AssistantMessage assistant) {
        List<ToolCallSpecJson> calls = assistant.getToolCalls() == null ? List.of()
                : assistant.getToolCalls().stream()
                        .map(tc -> new ToolCallSpecJson(tc.name(), tc.arguments()))
                        .toList();
        return new Exchange(
                RequestFingerprint.from(prompt),
                new ResponseSpec(assistant.getText() == null ? "" : assistant.getText(), calls));
    }

    /** 从文件加载 fixture。 */
    public static RecordingFixture load(Path path) {
        try {
            return new ObjectMapper().readValue(Files.readString(path), RecordingFixture.class);
        } catch (IOException e) {
            throw new UncheckedIOException("加载录制 fixture 失败: " + path, e);
        }
    }

    /** 从 classpath 的 {@code recordings/<name>.json} 加载 fixture。 */
    public static RecordingFixture loadClasspath(String name) {
        return load(Path.of("recordings", name + ".json"));
    }

    /** 写入 fixture（缩进 JSON）。 */
    public void writeTo(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Files.writeString(path, mapper.writeValueAsString(this));
        } catch (IOException e) {
            throw new UncheckedIOException("写录制 fixture 失败: " + path, e);
        }
    }

    /** 录制交换条数。 */
    public int size() {
        return exchanges.size();
    }
}
