package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话 → (appId, agentName) 绑定索引（skills 模块内部）。
 *
 * <p>{@code InjectionViewProcessor} 的注入视图构建只见 sessionId；技能清单渲染需 (appId, agentName)
 * 才能解析绑定。故 {@link SkillModule} 注册一个 {@code SessionResourceCustomizer}，在 spawn 时把
 * (sessionId, appId, agentName) 登记于此，{@link SkillCatalogRendererImpl} 据 sessionId 反查。
 *
 * <p>会话关闭时由 customizer 的清理钩子移除条目（防泄漏）。
 */
public class SessionBindingIndex {

    public record Binding(String appId, String agentName) {
    }

    private final ConcurrentHashMap<String, Binding> bindings = new ConcurrentHashMap<>();

    public void register(String sessionId, String appId, String agentName) {
        if (sessionId != null) {
            bindings.put(sessionId, new Binding(appId, agentName));
        }
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            bindings.remove(sessionId);
        }
    }

    public Optional<Binding> get(String sessionId) {
        return Optional.ofNullable(sessionId == null ? null : bindings.get(sessionId));
    }
}
