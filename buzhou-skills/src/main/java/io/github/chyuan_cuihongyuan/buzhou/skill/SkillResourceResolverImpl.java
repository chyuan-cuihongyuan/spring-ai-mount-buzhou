package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;

import java.util.Optional;

/**
 * {@link SkillResourceResolver} 实现（spec 04：read_range 接管 skill:// 路径的 skills 侧）。
 *
 * <p>先按会话绑定做可见性校验（与 {@code load_skill} 入参校验同源），再按名解析资源
 * （DB-PUBLISHED &gt; classpath）。装配期经 {@code SpillModule.skillResourceResolver} 注入。
 */
final class SkillResourceResolverImpl implements SkillResourceResolver {

    private final SkillRegistry registry;
    private final BindingVisibility visibility;

    SkillResourceResolverImpl(SkillRegistry registry, SessionBindingIndex bindingIndex) {
        this.registry = registry;
        this.visibility = new BindingVisibility(registry, bindingIndex);
    }

    @Override
    public Optional<String> resolve(String sessionId, String skillName, String relativePath) {
        if (!visibility.isVisible(sessionId, skillName)) {
            return Optional.empty();
        }
        // 按名解析（绑定校验已在上方完成；appId/agentName 对解析本身无影响）
        return registry.loadResource(null, null, skillName, relativePath);
    }
}
