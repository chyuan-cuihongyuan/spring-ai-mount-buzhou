package io.github.chyuan_cuihongyuan.buzhou.core.internal.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyChangeListener;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PropertiesPolicyConfigProvider implements PolicyConfigProvider {

    private final Map<String, BindingPolicy> policies;

    public PropertiesPolicyConfigProvider(Map<String, BindingPolicy> policies) {
        this.policies = Map.copyOf(policies);
    }

    @Override
    public BindingPolicy getBindingPolicy(String appId, String agentName) {
        return policies.getOrDefault(BindingPolicy.key(appId, agentName),
                BindingPolicy.empty(appId, agentName));
    }

    @Override
    public void addChangeListener(BindingPolicyChangeListener listener) {
    }
}
