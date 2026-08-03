package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

/**
 * 从 {@code AssistantMessage.getMetadata()} 提取的思维链结果（spec 03 思维链厂商适配）。
 *
 * @param content     思维链文本（被 {@code maxChars} 截断时为截断后文本）
 * @param providerKey 采集来源 metadata key（如 {@code reasoningContent}/{@code thinking}）
 * @param signature   Anthropic signature（续接回传需要），其他厂商为 null
 * @param omitted     Anthropic display=OMITTED 时为 true，仅元数据
 * @param truncated   超过 {@code maxChars} 截断时为 true
 * @param originalLength 原始思维链长度（截断前），未截断时等于 content 长度
 */
public record ExtractedThinking(
        String content,
        String providerKey,
        String signature,
        boolean omitted,
        boolean truncated,
        int originalLength) {

    public boolean isEmpty() {
        return (content == null || content.isBlank()) && !omitted;
    }
}
