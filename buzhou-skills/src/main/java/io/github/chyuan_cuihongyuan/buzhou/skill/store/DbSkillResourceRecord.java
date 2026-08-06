package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import java.time.Instant;

/** DB Skill 资源记录（spec 04 {@code buzhou_skill_resource} 表；仅文本 CLOB）。 */
public record DbSkillResourceRecord(Long id, String skillName, String relativePath,
                                    String mediaType, String content, long sizeBytes,
                                    Instant updatedAt) {

    public DbSkillResourceRecord {
        mediaType = mediaType == null || mediaType.isBlank() ? "text/plain" : mediaType;
        sizeBytes = sizeBytes < 0 ? (content == null ? 0 : content.length()) : sizeBytes;
    }
}
