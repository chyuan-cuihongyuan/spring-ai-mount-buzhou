package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link LoadSkillTool} 端到端单元测试（spec 04：返回正文 + 资源清单 / 失败转文本 / 绑定入参校验）。 */
class LoadSkillToolTest {

    private final SkillRegistry registry = new DefaultSkillRegistry(
            cast(new ClasspathSkillScanner().scan()), new InMemorySkillStore(), null, true, 64);
    private final LoadSkillTool tool = new LoadSkillTool(registry, new SessionBindingIndex());

    @Test
    void returnsBodyAndResourceCatalog() {
        String result = tool.call("{\"name\":\"code-review\"}");
        assertThat(result).contains("# Code Review Skill", "严重度分级");
        // 资源清单 + skill:// 路径指引
        assertThat(result).contains("checklists/security.md", "skill://code-review/");
    }

    @Test
    void skillWithoutResourcesOmitsResourceSection() {
        String result = tool.call("{\"name\":\"sql-tuning\"}");
        assertThat(result).contains("# SQL Tuning Skill");
        assertThat(result).doesNotContain("资源清单");
    }

    @Test
    void unknownSkillReturnsTextNotException() {
        assertThat(tool.call("{\"name\":\"nope\"}")).contains("技能不存在或未绑定");
    }

    @Test
    void missingNameParamReturnsText() {
        assertThat(tool.call("{}")).contains("缺少 name 参数");
    }

    @Test
    void blankInputReturnsText() {
        assertThat(tool.call("")).contains("缺少 name 参数");
    }

    @Test
    void malformedJsonReturnsText() {
        assertThat(tool.call("not json")).contains("参数解析失败");
    }

    @Test
    void toolDefinitionMatchesSpec() {
        assertThat(tool.getToolDefinition().name()).isEqualTo("load_skill");
        assertThat(tool.getToolDefinition().inputSchema()).contains("\"name\"");
    }

    // ---- 绑定入参校验（spec 04：name 必须在当前绑定清单内）----

    @Test
    void boundSessionLoadsBoundSkill() {
        InMemoryBindingPolicyStore bindingStore = new InMemoryBindingPolicyStore();
        bindingStore.save(new BindingPolicy("app", "agent", Map.of(), List.of("sql-tuning"), List.of(), 1));
        SkillRegistry boundRegistry = new DefaultSkillRegistry(cast(new ClasspathSkillScanner().scan()),
                null, new StoreBackedPolicyProvider(bindingStore), false, 64);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");
        LoadSkillTool boundTool = new LoadSkillTool(boundRegistry, index);

        String result = boundTool.call("{\"name\":\"sql-tuning\"}", contextOf("sess"));
        assertThat(result).contains("# SQL Tuning Skill");
    }

    @Test
    void boundSessionRejectsCroppedSkill() {
        InMemoryBindingPolicyStore bindingStore = new InMemoryBindingPolicyStore();
        bindingStore.save(new BindingPolicy("app", "agent", Map.of(), List.of("sql-tuning"), List.of(), 1));
        SkillRegistry boundRegistry = new DefaultSkillRegistry(cast(new ClasspathSkillScanner().scan()),
                null, new StoreBackedPolicyProvider(bindingStore), false, 64);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");
        LoadSkillTool boundTool = new LoadSkillTool(boundRegistry, index);

        // code-review 被绑定裁剪 → 入参校验拒绝，正文不带出
        String result = boundTool.call("{\"name\":\"code-review\"}", contextOf("sess"));
        assertThat(result).contains("技能不存在或未绑定");
        assertThat(result).doesNotContain("# Code Review Skill");
    }

    @Test
    void noSessionContextSkipsBindingCheck() {
        // 非会话内直调（ToolContext 无 sessionId）：不校验，按名解析放行
        assertThat(tool.call("{\"name\":\"code-review\"}", new ToolContext(Map.of())))
                .contains("# Code Review Skill");
    }

    private static ToolContext contextOf(String sessionId) {
        return new ToolContext(Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sessionId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> cast(
            Map<String, ?> raw) {
        return (Map) raw;
    }
}
