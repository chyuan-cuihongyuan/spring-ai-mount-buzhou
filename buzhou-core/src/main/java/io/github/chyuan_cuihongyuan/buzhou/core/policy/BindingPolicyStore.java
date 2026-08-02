package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Optional;

public interface BindingPolicyStore {

    void save(BindingPolicy policy);

    Optional<BindingPolicy> find(String appId, String agentName);
}
