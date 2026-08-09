package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.CompositeAttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

/**
 * 渐进式记忆压缩自装配（spec 01 / 09 / ticket 22）。
 *
 * <p>把 {@link MemoryModule#configure} 产出注册为 {@link RuntimeConfig} bean，供 core 装配收集合并。
 * 跨机制桥接（Attachment / SkillCatalog）经 core SPI 注入，缺失时降级：
 * <ul>
 *   <li>{@code AttachmentRenderer}（guard 事实 / tools todo）→ 多个则用
 *       {@link CompositeAttachmentRenderer} 组合，零个则 null；</li>
 *   <li>{@code SkillCatalogRenderer}（skills）→ {@link ObjectProvider} 可空；</li>
 *   <li>{@code ChatModel} → 主模型可空（无模型时仅微压缩 + 事实注入，不生成摘要），
 *       摘要模型经限定符 {@code buzhouSummaryChatModel} 可选，缺省复用主模型。</li>
 * </ul>
 *
 * <p>配置经 {@link ConfigMaps#sub(Environment, String)} 取 {@code buzhou} 子树（含
 * {@code model-name} / {@code memory.*} / {@code tool-policies} / {@code facts.*}）喂入既有解析。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.memory", name = "enabled", matchIfMissing = true)
public class BuzhouMemoryAutoConfiguration {

    @Bean
    public RuntimeConfig memoryRuntimeConfig(BuzhouStores stores, Environment env,
                                             List<AttachmentRenderer> attachmentRenderers,
                                             ObjectProvider<SkillCatalogRenderer> catalogRenderer,
                                             ObjectProvider<ChatModel> chatModel,
                                             @Qualifier("buzhouSummaryChatModel")
                                             ObjectProvider<ChatModel> summaryModel) {
        Map<String, Object> map = ConfigMaps.sub(env, "buzhou");
        AttachmentRenderer renderer = compose(attachmentRenderers);
        ChatModel main = chatModel.getIfAvailable();
        ChatModel summary = summaryModel.getIfAvailable();
        return MemoryModule.configure(map, stores, main, summary != null ? summary : main,
                renderer, catalogRenderer.getIfAvailable());
    }

    private static AttachmentRenderer compose(List<AttachmentRenderer> renderers) {
        if (renderers == null || renderers.isEmpty()) {
            return null;
        }
        if (renderers.size() == 1) {
            return renderers.get(0);
        }
        return new CompositeAttachmentRenderer(renderers);
    }
}
