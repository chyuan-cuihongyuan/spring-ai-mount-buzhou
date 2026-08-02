package io.github.chyuan_cuihongyuan.buzhou.core.internal.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBindingPolicyStore implements BindingPolicyStore {

    private final ConcurrentHashMap<String, BindingPolicy> policies = new ConcurrentHashMap<>();

    @Override
    public void save(BindingPolicy policy) {
        policies.put(BindingPolicy.key(policy.appId(), policy.agentName()), policy);
    }

    @Override
    public Optional<BindingPolicy> find(String appId, String agentName) {
        return Optional.ofNullable(policies.get(BindingPolicy.key(appId, agentName)));
    }
}
