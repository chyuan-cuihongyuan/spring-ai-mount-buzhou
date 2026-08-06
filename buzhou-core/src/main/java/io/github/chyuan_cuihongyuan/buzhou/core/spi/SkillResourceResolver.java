package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Optional;

/**
 * Skill 资源内容解析 SPI（spec 04）：skills → spill {@code read_range} 的跨机制桥。
 *
 * <p>{@code read_range} 收到 {@code skill://<name>/<relativePath>} 路径时委托此接口取资源内容；
 * 由 buzhou-skills 提供实现（含会话绑定可见性校验），buzhou-spill 装配期注入——
 * 同 {@link SkillCatalogRenderer}（skills → memory）模式，feature 模块间零 Maven 依赖。
 *
 * <p>实现方约定：{@code sessionId} 可空（非会话内直调）；会话已登记绑定时须校验
 * {@code skillName} 在该会话可见清单内，不可见/不存在一律返回 {@link Optional#empty()}。
 */
@FunctionalInterface
public interface SkillResourceResolver {

    /**
     * 解析 Skill 资源文本。
     *
     * @param sessionId    当前会话 id（可空：非会话内调用不做绑定校验）
     * @param skillName    技能名（skill:// 路径第一段）
     * @param relativePath 资源相对路径（skill:// 路径剩余段）
     * @return 资源文本；技能未绑定/资源不存在返回 empty
     */
    Optional<String> resolve(String sessionId, String skillName, String relativePath);
}
