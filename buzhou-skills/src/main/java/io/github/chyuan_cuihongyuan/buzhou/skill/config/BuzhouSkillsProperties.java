package io.github.chyuan_cuihongyuan.buzhou.skill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * skills 模块外层装配属性（spec 21 / T91 / impl-66，前缀 {@code buzhou.skills}）。
 * 模块内部细键仍走 {@code fromYml} map 契约（by-design，见 spec 21）。
 *
 * @param enabled   模块开关（默认开）
 * @param dbEnabled DB 动态 Skill 开关（存在 SkillStore bean 时生效；显式 false 可关闭）
 */
@ConfigurationProperties(prefix = "buzhou.skills")
public record BuzhouSkillsProperties(
        Boolean enabled,
        Boolean dbEnabled) {

    public BuzhouSkillsProperties {
        enabled = enabled == null || enabled;
        dbEnabled = dbEnabled == null || dbEnabled;
    }
}
