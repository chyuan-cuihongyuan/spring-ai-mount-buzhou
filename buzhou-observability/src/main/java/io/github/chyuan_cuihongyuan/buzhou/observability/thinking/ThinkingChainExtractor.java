package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 思维链厂商适配（spec 03 思维链厂商适配表）。
 *
 * <p>从 {@link AssistantMessage#getMetadata()} 按厂商适配表提取思维链，统一产出
 * {@link ExtractedThinking}。适配表内置且可经 {@code extraKeys} 配置扩展。
 *
 * <p>内置 key（按 spec 适配表）：
 * <ul>
 *   <li>{@code reasoningContent}（OpenAI 兼容 / DeepSeek / vLLM / Ollama 的 OpenAI 端点）</li>
 *   <li>{@code thinking}（Ollama 原生端点）</li>
 *   <li>{@code thinking_content}（Mistral；与 {@code reference_thinking_content} 合并）</li>
 *   <li>{@code thoughts}（Google GenAI，options ThinkingConfig.includeThoughts 开启才返回）</li>
 *   <li>{@code reasoning_signature}（Anthropic signature，与 thinking 文本配对）</li>
 * </ul>
 *
 * <p>超长处理：超过 {@code maxChars} 截断并置 {@code truncated=true} + 记原始长度（本票不接 Spill 管道，
 * 见 ticket 11 Comments 推演偏离）。
 */
public class ThinkingChainExtractor {

    /** Anthropic display=OMITTED 标记：仅元数据、思维链文本不返回。 */
    public static final String ATTR_OMITTED = "thinking_omitted";

    /** Google GenAI thoughts metadata key。 */
    public static final String KEY_GOOGLE_THOUGHTS = "thoughts";

    private final List<String> orderedKeys;
    private final int maxChars;

    public ThinkingChainExtractor(List<String> extraKeys, int maxChars) {
        List<String> keys = new ArrayList<>();
        // 顺序：reasoningContent > thinking > thinking_content > thoughts，最后 extraKeys
        keys.add("reasoningContent");
        keys.add("thinking");
        keys.add("thinking_content");
        keys.add(KEY_GOOGLE_THOUGHTS);
        if (extraKeys != null) {
            for (String k : extraKeys) {
                if (k != null && !k.isBlank() && !keys.contains(k)) {
                    keys.add(k);
                }
            }
        }
        this.orderedKeys = List.copyOf(keys);
        this.maxChars = Math.max(1, maxChars);
    }

    /**
     * 从助手消息提取思维链。无任何已知 key 时返回 {@link Optional#empty()}（采集方据此决定是否
     * 标 {@code thinking.available=PROVIDER_NOT_RETURNED}）。
     */
    public Optional<ExtractedThinking> extract(AssistantMessage message) {
        if (message == null) {
            return Optional.empty();
        }
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return Optional.empty();
        }
        boolean omitted = Boolean.TRUE.equals(metadata.get(ATTR_OMITTED))
                || "true".equalsIgnoreCase(String.valueOf(metadata.get(ATTR_OMITTED)));
        String signature = stringOf(metadata.get("reasoning_signature"));
        for (String key : orderedKeys) {
            Object raw = metadata.get(key);
            String text = stringOf(raw);
            if (text != null && !text.isBlank()) {
                // Mistral：reference_thinking_content 与 thinking_content 合并（spec 03 适配表）
                if ("thinking_content".equals(key)) {
                    String reference = stringOf(metadata.get("reference_thinking_content"));
                    if (reference != null && !reference.isBlank()) {
                        text = text + "\n---\n" + reference;
                    }
                }
                return Optional.of(build(key, text, signature, omitted));
            }
        }
        if (omitted) {
            // 仅元数据（Anthropic display=OMITTED）
            return Optional.of(new ExtractedThinking("", "reasoning_signature", signature, true, false, 0));
        }
        return Optional.empty();
    }

    private ExtractedThinking build(String providerKey, String text, String signature, boolean omitted) {
        int originalLength = text.length();
        boolean truncated = originalLength > maxChars;
        String content = truncated ? text.substring(0, maxChars) : text;
        return new ExtractedThinking(content, providerKey, signature, omitted, truncated, originalLength);
    }

    private String stringOf(Object raw) {
        return raw instanceof String s && !s.isBlank() ? s : null;
    }
}
