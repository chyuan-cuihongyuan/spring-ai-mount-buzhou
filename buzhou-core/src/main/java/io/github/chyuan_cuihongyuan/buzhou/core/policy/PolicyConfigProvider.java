package io.github.chyuan_cuihongyuan.buzhou.core.policy;

public interface PolicyConfigProvider {

    BindingPolicy getBindingPolicy(String appId, String agentName);

    void addChangeListener(BindingPolicyChangeListener listener);
}
