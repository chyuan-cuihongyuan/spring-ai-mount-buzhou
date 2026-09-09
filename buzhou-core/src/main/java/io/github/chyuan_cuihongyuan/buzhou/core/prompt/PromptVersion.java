package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.time.Instant;

/**
 * 提示词版本快照（spec 401 / T693，Langfuse prompt management 借鉴）：
 * 发布即不可变——name/version/body/note/publishedAt 五元组。标签指针存
 * 注册表态（指针移动不产生新版本）。
 */
public record PromptVersion(String name, int version, String body, String note,
        Instant publishedAt) {

    public PromptVersion {
        body = body == null ? "" : body;
        note = note == null ? "" : note;
        publishedAt = publishedAt == null ? Instant.EPOCH : publishedAt;
    }
}
