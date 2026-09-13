package io.github.chyuan_cuihongyuan.buzhou.skill;

/**
 * impl-769 / spec 1016：模型面技能解析只读快照（Berkeley function-calling
 * leaderboard 借鉴——幻觉技能名是提示词清单呈现质量的直读信号）。
 *
 * <p>守恒不变量：{@code loads == resolved + notFound}（仅 load() 口径，
 * 清单枚举路径不计）。
 *
 * @param loads     load() 调用累计
 * @param resolved  解析命中累计
 * @param notFound  解析为空累计（classpath+DB 双缺——幻觉技能名信号）
 */
public record SkillResolutionStats(long loads, long resolved, long notFound) {
}
