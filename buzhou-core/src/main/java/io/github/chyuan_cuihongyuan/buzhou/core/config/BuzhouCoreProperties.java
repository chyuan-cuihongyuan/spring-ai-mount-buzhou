package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内核装配属性（spec 09 配置项表，前缀 {@code buzhou}）。
 *
 * <p>承载 store 实现选择（{@code buzhou.store.type}）与模型名（{@code buzhou.model-name}，
 * 供 memory / observability 等模块共享，避免重复绑定）。
 *
 * @param modelName 模型名，默认 {@code unknown}（对齐 {@code MemoryModule} 默认）
 * @param store     store 选择；默认 {@code memory}
 */
@ConfigurationProperties(prefix = "buzhou")
public record BuzhouCoreProperties(String modelName, Store store) {

    public BuzhouCoreProperties {
        modelName = (modelName == null || modelName.isBlank()) ? "unknown" : modelName;
        store = (store == null) ? new Store(null) : store;
    }

    public record Store(String type) {
        public Store {
            type = (type == null || type.isBlank()) ? "memory" : type;
        }
    }
}
