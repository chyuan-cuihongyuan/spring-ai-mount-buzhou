package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * skill_search 测试（spec 37 §A / T132 / impl-105）：子串命中（名称/描述）、
 * 不受注入上限限制（截断目录外的技能可检索）、绑定可见性、上限与空结果口径。
 */
class SkillSearchToolTest {

    private DefaultSkillRegistry registry(BindingPolicyStore store, int maxEntries) {
        return new DefaultSkillRegistry(cast(new ClasspathSkillScanner().scan()), null,
                store == null ? null : new StoreBackedPolicyProvider(store), false, maxEntries);
    }

    /** 命中：描述子串检索 + load_skill 指引；注入上限外技能仍可检索。 */
    @Test
    void searchFindsSkillsBeyondCatalogCap() {
        DefaultSkillRegistry registry = registry(null, 1); // 目录注入上限 1
        SkillSearchTool tool = new SkillSearchTool(registry, new SessionBindingIndex());

        // 目录被截断为 1 条（含溢出提示）——但检索走全集
        assertThat(registry.listForPage(null, null).entries()).hasSize(1);
        assertThat(registry.listAllFor(null, null).size()).isGreaterThan(1);

        String result = tool.call("{\"query\":\"sql\"}", (ToolContext) null);

        assertThat(result).contains("load_skill(name)").contains("sql-tuning");
    }

    /** 名称子串不分大小写；无命中给可操作提示。 */
    @Test
    void caseInsensitiveAndNoHit() {
        SkillSearchTool tool = new SkillSearchTool(registry(null, 64), new SessionBindingIndex());

        assertThat(tool.call("{\"query\":\"CODE\"}", (ToolContext) null))
                .contains("code-review");
        assertThat(tool.call("{\"query\":\"不存在的东西xyz\"}", (ToolContext) null))
                .contains("无匹配技能");
    }

    /** 绑定可见性：未绑定会话检索不到裁剪技能。 */
    @Test
    void bindingVisibilityFiltersResults() {
        BindingPolicyStore store = new InMemoryBindingPolicyStore();
        store.save(new BindingPolicy("app", "agent", Map.of(), List.of("sql-tuning"), List.of(), 1));
        DefaultSkillRegistry registry = registry(store, 64);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");
        SkillSearchTool tool = new SkillSearchTool(registry, index);

        String result = tool.call("{\"query\":\"code\"}",
                new ToolContext(Map.of("buzhou.sessionId", "sess")));

        assertThat(result).contains("无匹配技能"); // code-review 未绑定
        assertThat(tool.call("{\"query\":\"sql\"}",
                new ToolContext(Map.of("buzhou.sessionId", "sess")))).contains("sql-tuning");
    }

    /** 参数容错：缺 query / 坏 JSON 给清晰提示。 */
    @Test
    void argumentValidation() {
        SkillSearchTool tool = new SkillSearchTool(registry(null, 64), new SessionBindingIndex());
        assertThat(tool.call("{}", (ToolContext) null)).contains("缺少 query");
        assertThat(tool.call("not-json", (ToolContext) null)).contains("参数解析失败");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> cast(
            Map<String, ?> raw) {
        return (Map) raw;
    }
}
