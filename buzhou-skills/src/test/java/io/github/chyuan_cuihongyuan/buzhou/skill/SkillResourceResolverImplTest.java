package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SkillModule#skillResourceResolver()} 测试（spec 04：skill:// 解析的 skills 侧，
 * 与 load_skill 同源的绑定可见性校验 + DB 覆盖资源解析）。
 * 会话级绑定拒绝的端到端见 examples 聚合侧 SkillIntegrationTest。
 */
class SkillResourceResolverImplTest {

    @Test
    void resolvesClasspathResourceForUnboundSession() {
        SkillModule skills = SkillModule.builder().build();
        SkillResourceResolver resolver = skills.skillResourceResolver();

        // 未绑定 = 全部 classpath 可见（索引未登记的会话按非会话内处理，不校验）
        assertThat(resolver.resolve("sess", "code-review", "checklists/security.md"))
                .hasValueSatisfying(v -> assertThat(v).contains("输入是否经校验/转义"));
    }

    @Test
    void unknownResourceReturnsEmpty() {
        SkillModule skills = SkillModule.builder().build();
        assertThat(skills.skillResourceResolver().resolve(null, "code-review", "nope.md")).isEmpty();
    }

    @Test
    void dbOverrideResourcePreferred() {
        SkillModule skills = SkillModule.builder().dbEnabled(true).build();
        skills.skillAdminApi().create("code-review", "DB 版", "正文", List.of(), "ops");
        skills.skillAdminApi().publish("code-review");
        skills.skillAdminApi().uploadResource("code-review", "checklists/security.md",
                "DB 资源内容", "text/markdown");

        assertThat(skills.skillResourceResolver().resolve(null, "code-review", "checklists/security.md"))
                .contains("DB 资源内容");
    }

    @Test
    void disabledModuleReturnsNullResolver() {
        assertThat(SkillModule.builder().enabled(false).build().skillResourceResolver()).isNull();
    }
}
