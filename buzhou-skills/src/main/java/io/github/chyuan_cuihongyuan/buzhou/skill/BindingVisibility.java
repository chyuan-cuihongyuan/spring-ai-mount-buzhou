package io.github.chyuan_cuihongyuan.buzhou.skill;

/**
 * 会话级技能可见性判定（spec 04 入参校验：name 必须在当前绑定清单内）。
 *
 * <p>{@code load_skill} 与 {@code skill://} 资源解析共用。会话已在 {@link SessionBindingIndex}
 * 登记时严格校验；sessionId 缺失或索引未登记（非会话内直调工具的异常路径）放行按名解析——
 * 绑定校验保护的是 harness 内的模型调用面，直调不属于该面。
 */
final class BindingVisibility {

    private final SkillRegistry registry;
    private final SessionBindingIndex bindingIndex;

    BindingVisibility(SkillRegistry registry, SessionBindingIndex bindingIndex) {
        this.registry = registry;
        this.bindingIndex = bindingIndex;
    }

    boolean isVisible(String sessionId, String skillName) {
        if (sessionId == null || bindingIndex == null) {
            return true;
        }
        return bindingIndex.get(sessionId)
                .map(b -> registry.isVisibleFor(b.appId(), b.agentName(), skillName))
                .orElse(true);
    }
}
