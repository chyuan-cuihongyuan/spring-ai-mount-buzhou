package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.List;

/**
 * 清单条目：进系统提示词的最小信息（spec 04）。
 *
 * <p>仅 {@code name + description} 注入 Skill Catalog；{@code allowedTools} 现阶段仅作
 * 提示词层面引导（spec 04 开放问题：是否在工具解析层硬过滤未定）。
 */
public record SkillMetadata(String name, String description,
                            List<String> allowedTools, SkillSource source) {

    public SkillMetadata {
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    }
}
