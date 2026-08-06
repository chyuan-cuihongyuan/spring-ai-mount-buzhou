package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;

import java.util.List;
import java.util.Optional;

/**
 * {@link SkillCatalogRenderer} 实现：据 sessionId 反查 (appId, agentName) 绑定，
 * 经 {@link SkillRegistry#listFor} 取可见清单，渲染为系统提示词尾部注入文本（spec 04）。
 *
 * <p>每轮注入视图构建时现取——上架/解绑/改绑定下一轮即生效，无需重建会话。
 */
public class SkillCatalogRendererImpl implements SkillCatalogRenderer {

    private final SessionBindingIndex index;
    private final SkillRegistry registry;

    public SkillCatalogRendererImpl(SessionBindingIndex index, SkillRegistry registry) {
        this.index = index;
        this.registry = registry;
    }

    @Override
    public Optional<String> renderCatalog(String sessionId) {
        SessionBindingIndex.Binding binding = index.get(sessionId).orElse(null);
        if (binding == null) {
            return Optional.empty();
        }
        List<SkillMetadata> catalog = registry.listFor(binding.appId(), binding.agentName());
        if (catalog.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## 可用技能（Skill Catalog）\n");
        sb.append("以下技能可按需调用 load_skill(name) 加载正文（name 即清单首列）：\n");
        for (SkillMetadata meta : catalog) {
            sb.append("- ").append(meta.name());
            if (meta.description() != null && !meta.description().isBlank()) {
                sb.append(": ").append(meta.description());
            }
            sb.append('\n');
        }
        return Optional.of(sb.toString().strip());
    }
}
