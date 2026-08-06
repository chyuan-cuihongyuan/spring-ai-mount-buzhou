package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyChangeListener;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 读穿适配器：把 {@link BindingPolicyStore} 暴露为 {@link PolicyConfigProvider}（skills 内部）。
 *
 * <p>{@link SkillRegistry} 每轮渲染清单时 {@code getBindingPolicy} 现取，故无需 DbPolicyConfigProvider
 * 的轮询/变更监听机制——绑定写入后下一轮读取即见。监听器接口置为空实现（无推送需求）。
 */
class StoreBackedPolicyProvider implements PolicyConfigProvider {

    private final BindingPolicyStore store;
    private final CopyOnWriteArrayList<BindingPolicyChangeListener> listeners = new CopyOnWriteArrayList<>();

    StoreBackedPolicyProvider(BindingPolicyStore store) {
        this.store = store;
    }

    @Override
    public BindingPolicy getBindingPolicy(String appId, String agentName) {
        return store.find(appId, agentName).orElse(BindingPolicy.empty(appId, agentName));
    }

    @Override
    public void addChangeListener(BindingPolicyChangeListener listener) {
        listeners.add(listener);
    }
}
