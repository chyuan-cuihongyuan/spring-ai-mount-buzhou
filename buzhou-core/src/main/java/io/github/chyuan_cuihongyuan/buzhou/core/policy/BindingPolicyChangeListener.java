package io.github.chyuan_cuihongyuan.buzhou.core.policy;

@FunctionalInterface
public interface BindingPolicyChangeListener {

    void onChange(BindingPolicy policy);
}
