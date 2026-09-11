package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import java.util.Map;

/**
 * 模型能力注册表（spec 502 / T755，LiteLLM Router capabilities 借鉴）：
 * 模型名 → 能力声明的只读查询面。未注册模型返回 null（调用方按零门
 * 透传——声明渐进、不声明不误拦）。
 */
public final class ModelCapabilityRegistry {

    private final Map<String, ModelCapabilities> models;

    public ModelCapabilityRegistry(Map<String, ModelCapabilities> models) {
        this.models = models == null ? Map.of() : Map.copyOf(models);
    }

    /** 查询模型能力；未注册返回 null（零门透传语义）。 */
    public ModelCapabilities of(String modelName) {
        return modelName == null ? null : models.get(modelName);
    }

    /** 已注册模型数（观测/装配判定）。 */
    public int size() {
        return models.size();
    }
}
