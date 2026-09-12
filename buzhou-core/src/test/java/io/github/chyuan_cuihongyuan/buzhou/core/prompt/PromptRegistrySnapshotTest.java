package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 545 / T843：注册表快照导出/导入——导出还原往返（版本号/正文/标签
 * 指针保持）、非空目标 fail-fast、坏 JSON fail-fast（Langfuse export/import）。
 */
class PromptRegistrySnapshotTest {

    private static InMemoryPromptRegistry seeded() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好 v1", "初版");
        registry.publish("greeting", "你好 v2", "润色");
        registry.label("greeting", "production", 1);
        registry.publish("policy", "礼貌用语", "policy 初版");
        return registry;
    }

    @Test
    void exportImportRoundTripPreservesVersionsAndLabels() {
        InMemoryPromptRegistry source = seeded();
        String json = PromptRegistrySnapshot.export(source);
        assertThat(json).contains("buzhou.prompt-registry-snapshot");

        InMemoryPromptRegistry restored = new InMemoryPromptRegistry();
        PromptRegistrySnapshot.importInto(restored, json);

        // 版本号保持（重放后 1/2 对齐）
        assertThat(restored.versions("greeting")).hasSize(2);
        assertThat(restored.resolveVersion("greeting", 1).orElseThrow().body()).isEqualTo("你好 v1");
        assertThat(restored.resolveVersion("greeting", 2).orElseThrow().body()).isEqualTo("你好 v2");
        // 标签指针保持：production → v1（旧行为复现）
        assertThat(restored.resolve("greeting", "production").orElseThrow().body()).isEqualTo("你好 v1");
        // latest 自动指向最新
        assertThat(restored.resolve("policy").orElseThrow().body()).isEqualTo("礼貌用语");
        assertThat(restored.names()).containsExactlyInAnyOrder("greeting", "policy");
    }

    @Test
    void importIntoNonEmptyRegistryFailsFast() {
        InMemoryPromptRegistry source = seeded();
        String json = PromptRegistrySnapshot.export(source);
        InMemoryPromptRegistry occupied = seeded(); // 非空
        assertThatThrownBy(() -> PromptRegistrySnapshot.importInto(occupied, json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非空");
    }

    @Test
    void malformedJsonFailsFast() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        assertThatThrownBy(() -> PromptRegistrySnapshot.importInto(registry, "{bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptRegistrySnapshot.importInto(registry, "{\"format\":\"other\"}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptRegistrySnapshot.importInto(registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullRegistryFailFast() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        assertThatThrownBy(() -> PromptRegistrySnapshot.export(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PromptRegistrySnapshot.export(registry)).contains("buzhou.prompt-registry-snapshot");
    }
}
