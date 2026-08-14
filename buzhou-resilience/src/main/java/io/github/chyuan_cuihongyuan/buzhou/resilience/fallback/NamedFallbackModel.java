package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.springframework.ai.chat.model.ChatModel;

/**
 * 备模型条目（spec 15「备模型降级链」，T82 / impl-57）：name = 熔断/限流分桶键与
 * {@code buzhou.resilience.fallback.models} 的 bean 名；model = 降级时直接调用的备模型。
 *
 * @param name  备模型名（Spring 路径 = bean 名；与 modelName 分桶口径一致）
 * @param model 备模型实例
 */
public record NamedFallbackModel(String name, ChatModel model) {

    public NamedFallbackModel {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("NamedFallbackModel.name 不能为空（熔断/限流分桶键）");
        }
        if (model == null) {
            throw new IllegalArgumentException("NamedFallbackModel.model 不能为空");
        }
    }
}
