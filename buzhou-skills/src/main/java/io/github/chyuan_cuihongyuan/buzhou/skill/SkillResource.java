package io.github.chyuan_cuihongyuan.buzhou.skill;

/** Skill 资源（脚本/模板等），相对 {@code SKILL.md} 的路径标识（spec 04）。 */
public record SkillResource(String relativePath, long sizeBytes, String mediaType) {

    public SkillResource {
        mediaType = mediaType == null || mediaType.isBlank() ? "text/plain" : mediaType;
    }
}
