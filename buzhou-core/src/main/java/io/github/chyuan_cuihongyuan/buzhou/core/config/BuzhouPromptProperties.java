package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 提示词注册表 yml 播种（spec 401 / T694，Langfuse 借鉴）：
 * {@code buzhou.prompt.templates[{name, body, label}]}。同 name 同 body
 * 幂等跳过（重启不掀版本）；声明 label 自动指向该名当前最新。
 */
@ConfigurationProperties(prefix = "buzhou.prompt")
public record BuzhouPromptProperties(List<TemplateSpec> templates) {

    public BuzhouPromptProperties {
        templates = templates == null ? List.of() : List.copyOf(templates);
    }

    /** 播种条目（label 可空——只 publish 不打标）。 */
    public record TemplateSpec(String name, String body, String label) {
    }
}
