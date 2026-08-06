package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ReadRangeTool} 接管 {@code skill://} 路径测试（spec 04 推演：
 * 资源读取复用 read_range，委托 core SPI {@link SkillResourceResolver}；仅支持 bytes 模式）。
 * 解析器用桩实现——spill 不依赖 skills（feature 模块间零依赖）。
 */
class ReadRangeToolSkillUriTest {

    @TempDir
    Path spillDir;

    private final SkillResourceResolver stubResolver = (sessionId, skillName, relativePath) ->
            "code-review".equals(skillName) && "checklists/security.md".equals(relativePath)
                    ? Optional.of("0123456789abcdef") : Optional.empty();

    private ReadRangeTool toolWith(SkillResourceResolver resolver) {
        SpillModule module = SpillModule.withDefaults(spillDir);
        return new ReadRangeTool(module.service(), resolver);
    }

    @Test
    void resolvesSkillResourceContent() {
        String result = toolWith(stubResolver)
                .call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}");
        assertThat(result).isEqualTo("0123456789abcdef");
    }

    @Test
    void bytesModeAppliesOffsetAndLimit() {
        String result = toolWith(stubResolver)
                .call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\",\"offset\":4,\"limit\":4}");
        assertThat(result).startsWith("4567").contains("已截断");
    }

    @Test
    void unknownResourceReturnsText() {
        String result = toolWith(stubResolver)
                .call("{\"path\":\"skill://code-review/nope.md\",\"mode\":\"bytes\"}");
        assertThat(result).contains("技能资源不存在或未绑定");
    }

    @Test
    void unwiredResolverReturnsHint() {
        String result = toolWith(null)
                .call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}");
        assertThat(result).contains("未接线");
    }

    @Test
    void nonBytesModeRejectedForSkillUri() {
        String result = toolWith(stubResolver)
                .call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"json\"}");
        assertThat(result).contains("仅支持 bytes 模式");
    }

    @Test
    void malformedSkillUriReturnsText() {
        String result = toolWith(stubResolver).call("{\"path\":\"skill://code-review\",\"mode\":\"bytes\"}");
        assertThat(result).contains("缺少资源相对路径");
    }
}
