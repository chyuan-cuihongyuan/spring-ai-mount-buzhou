package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Map;

/**
 * 注入快照中的消息（ticket 15 注入快照格式）。
 *
 * <p>用于后台还原"模型当时实际看到什么"——被微压缩替换的消息显示占位符与 evidence-id，
 * 被 spill 替换的消息显示引用句柄，可点击回查原文。
 *
 * @param role        角色（USER / ASSISTANT / SYSTEM / TOOL）
 * @param content     正文（或占位符/引用句柄）
 * @param evidenceId  微压缩证据指针（被压缩的消息）
 * @param spillUri    Spill 引用句柄 URI
 * @param metadata    其他元数据（tool_call_id 等）
 */
public record SnapshotMessage(
        String role,
        String content,
        String evidenceId,
        String spillUri,
        Map<String, Object> metadata) {

    public SnapshotMessage {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
