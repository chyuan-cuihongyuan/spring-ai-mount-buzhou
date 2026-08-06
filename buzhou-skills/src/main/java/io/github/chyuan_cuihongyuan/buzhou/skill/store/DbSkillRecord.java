package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;

import java.time.Instant;
import java.util.List;

/**
 * DB 动态 Skill 主表记录（spec 04 {@code buzhou_skill} 表）。
 *
 * <p>与内置 Skill 同名即覆盖（仅 {@link SkillStatus#PUBLISHED} 参与运行时解析）。
 * {@code version} 乐观锁兜底管理 API 并发编辑。
 */
public record DbSkillRecord(Long id, String name, String description, List<String> allowedTools,
                            String body, SkillStatus status, String createdBy,
                            Instant createdAt, Instant updatedAt, int version) {

    public DbSkillRecord {
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
        status = status == null ? SkillStatus.DRAFT : status;
    }
}
