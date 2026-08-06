package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.List;

/** Skill 全文：{@code load_skill} 的返回载荷（spec 04）。 */
public record Skill(String name, String description, List<String> allowedTools,
                    String body, List<SkillResource> resources, SkillSource source) {

    public Skill {
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
