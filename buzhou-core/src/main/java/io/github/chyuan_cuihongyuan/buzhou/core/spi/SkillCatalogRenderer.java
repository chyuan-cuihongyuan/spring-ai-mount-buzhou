package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Optional;

/**
 * 技能清单（Skill Catalog）注入渲染桥接（spec 04 注入机制）。
 *
 * <p>{@code buzhou-skills} 实现此接口：按当前会话的 {@code (appId, agentName)} 绑定解析可见
 * Skill 清单（{@code name + description}），渲染为注入文本。{@code buzhou-memory} 的注入视图
 * 构建方（{@code InjectionViewProcessor}）持有可选引用，在摘要块/事实块之后以
 * {@code <system-reminder>} 块注入系统提示词尾部——与 {@link AttachmentRenderer}（事实块）
 * 同一通道、同一「系统侧固定扣除」预算口径。
 *
 * <p>返回 {@link Optional#empty()} 表示当前会话无可见技能（未加载 skills 模块 / 无绑定且无内置 / 清单为空）。
 *
 * <p>键为 {@code sessionId} 而非 {@code (appId, agentName)}：注入视图构建方（memory）只见 sessionId，
 * {@code (appId, agentName)} 由 skills 侧的会话绑定索引在 spawn 时登记后反查（见 SkillModule）。
 */
@FunctionalInterface
public interface SkillCatalogRenderer {

    /**
     * 渲染当前会话可见的技能清单文本（不含 {@code <system-reminder>} 包裹，由调用方包装）。
     *
     * @param sessionId 会话 id
     */
    Optional<String> renderCatalog(String sessionId);
}
