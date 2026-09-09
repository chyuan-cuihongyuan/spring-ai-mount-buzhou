package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 424 §Testing / T739–T740：使用统计——三 resolve 形态记账、miss/
 * publish/label 零记账、透传语义、快照字典序、JSONL 两轮追加、yml 开关。
 */
class PromptUsageStatsTest {

    private static UsageTrackingPromptRegistry tracked(InMemoryPromptRegistry base,
            PromptUsageStats stats) {
        return new UsageTrackingPromptRegistry(base, stats);
    }

    @Test
    void shouldCountResolvesOnly_andPassThroughSemantics() {
        InMemoryPromptRegistry base = new InMemoryPromptRegistry();
        PromptUsageStats stats = new PromptUsageStats();
        UsageTrackingPromptRegistry registry = tracked(base, stats);

        base.publish("greet", "v1 body", null);
        base.publish("greet", "v2 body", null); // latest → 2
        base.publish("farewell", "f1", null);
        base.label("greet", "production", 1);

        // 三形态命中各记账
        assertThat(registry.resolve("greet")).hasValueSatisfying(v -> assertThat(v.version()).isEqualTo(2));
        assertThat(registry.resolve("greet", "production")).hasValueSatisfying(v -> assertThat(v.version()).isEqualTo(1));
        assertThat(registry.resolveVersion("greet", 2)).isPresent();
        // miss 零记账
        assertThat(registry.resolve("missing")).isEmpty();
        assertThat(registry.resolve("greet", "no-such-label")).isEmpty();
        assertThat(registry.resolveVersion("greet", 99)).isEmpty();

        var rows = stats.snapshot();
        assertThat(rows).containsExactly(
                new PromptUsageStats.Row("greet", 1, 1),
                new PromptUsageStats.Row("greet", 2, 2));

        // publish/label 不是使用——计数不动
        base.publish("greet", "v3 body", null);
        base.label("greet", "staging", 3);
        assertThat(stats.snapshot()).hasSize(2);

        // 透传语义：标签表/版本史/名集与裸 registry 一致
        assertThat(registry.labels("greet")).containsEntry("production", 1).containsEntry("latest", 3);
        assertThat(registry.versions("greet")).hasSize(3);
        assertThat(registry.names()).containsExactlyInAnyOrder("greet", "farewell");
    }

    @Test
    void shouldAppendSnapshotJsonl(@TempDir Path dir) throws Exception {
        InMemoryPromptRegistry base = new InMemoryPromptRegistry();
        PromptUsageStats stats = new PromptUsageStats();
        UsageTrackingPromptRegistry registry = tracked(base, stats);

        base.publish("greet", "v1", null);
        base.publish("greet", "v2", null);
        registry.resolve("greet");
        registry.resolve("greet");
        registry.resolveVersion("greet", 1);

        Path jsonl = dir.resolve("usage.jsonl");
        assertThat(PromptUsageJsonl.appendSnapshot(jsonl, stats)).isEqualTo(2);
        assertThat(PromptUsageJsonl.appendSnapshot(jsonl, stats)).isEqualTo(2); // 追加不清零

        List<String> lines = Files.readAllLines(jsonl);
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).contains("\"name\":\"greet\"").contains("\"version\":1").contains("\"count\":1");
        assertThat(lines.get(1)).contains("\"version\":2").contains("\"count\":2");
        assertThat(lines.get(3)).contains("\"count\":2"); // 第二轮同累计
    }

    @Test
    void shouldAssembleDecoratorOnlyWhenEnabled() {
        // 默认关：原样 InMemory（非装饰器）+ stats bean 恒在
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouPromptUsageStats");
                    assertThat(context.getBean(io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptRegistry.class))
                            .isNotInstanceOf(UsageTrackingPromptRegistry.class);
                });
        // 开：装饰器 + 记账真发生
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.prompt.usage-tracking.enabled=true",
                        "buzhou.prompt.templates[0].name=greet",
                        "buzhou.prompt.templates[0].body=你好")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var registry = context.getBean(io.github.chyuan_cuihongyuan.buzhou.core.prompt.PromptRegistry.class);
                    assertThat(registry).isInstanceOf(UsageTrackingPromptRegistry.class);
                    registry.resolve("greet");
                    var stats = context.getBean(PromptUsageStats.class);
                    assertThat(stats.snapshot()).containsExactly(
                            new PromptUsageStats.Row("greet", 1, 1));
                });
    }
}
