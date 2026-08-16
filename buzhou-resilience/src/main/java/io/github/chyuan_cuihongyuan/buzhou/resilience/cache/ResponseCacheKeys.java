package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 缓存键计算（spec 53 §A / T203）：sha256(modelName ‖ messages 规范序列化 ‖ options 采样)。
 *
 * <p>options 采样近似性（诚实入档）：只采 temperature/topP/topK/maxTokens 与 options 类名——
 * 自定义 provider 参数变化不破键；进程内缓存生命周期内 options 稳定是部署常态。
 * messages = memory advisor 注入后视图（键天然含会话历史）。
 */
public final class ResponseCacheKeys {

    private ResponseCacheKeys() {
    }

    /** 计算请求键（prompt 为空 messages 时仍可计算——空请求键确定）。 */
    public static String keyOf(String modelName, Prompt prompt) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("model=").append(modelName == null ? "" : modelName).append('\n');
        if (prompt != null) {
            List<Message> messages = prompt.getInstructions();
            if (messages != null) {
                for (Message m : messages) {
                    sb.append("msg{").append(m.getMessageType()).append('|')
                            .append(nullSafe(m.getText() == null ? contentOf(m) : m.getText()))
                            .append('|').append(nullSafe(idOf(m))).append("}\n");
                }
            }
            sb.append(optionsSample(prompt.getOptions()));
        }
        return sha256(sb.toString());
    }

    /** options 采样（未采样的自定义参数不破键——近似性入档）。 */
    static String optionsSample(ChatOptions options) {
        if (options == null) {
            return "opts=none";
        }
        return "opts{" + options.getClass().getSimpleName()
                + "|t=" + options.getTemperature()
                + "|topP=" + options.getTopP()
                + "|topK=" + options.getTopK()
                + "|max=" + options.getMaxTokens()
                + "|model=" + nullSafe(options.getModel())
                + "}";
    }

    private static String contentOf(Message m) {
        if (m instanceof ToolResponseMessage trm && trm.getResponses() != null) {
            // 工具响应：按响应 id+内容拼接（同工具同结果 → 同键；结果变 → 键变）
            StringBuilder sb = new StringBuilder();
            trm.getResponses().forEach(r -> sb.append(r.id()).append('=')
                    .append(r.responseData()).append(';'));
            return sb.toString();
        }
        if (m instanceof AssistantMessage am) {
            return nullSafe(am.getText());
        }
        return "";
    }

    private static String idOf(Message m) {
        // toolCallId 对 ToolResponseMessage 无单值语义；AssistantMessage 的 toolCalls 签名入键
        if (m instanceof AssistantMessage am && am.hasToolCalls()) {
            StringBuilder sb = new StringBuilder("tc[");
            am.getToolCalls().forEach(tc -> sb.append(tc.id()).append(':')
                    .append(tc.name()).append(':').append(tc.arguments()).append(';'));
            return sb.append(']').toString();
        }
        return "";
    }

    private static String nullSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
