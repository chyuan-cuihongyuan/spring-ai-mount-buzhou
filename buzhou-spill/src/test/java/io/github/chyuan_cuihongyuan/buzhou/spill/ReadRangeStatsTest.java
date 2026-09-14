package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1062 / impl 814：read_range 回读判定读面——spill 回读两桶（完整/截断）、
 * skill 资源读取与拒绝两桶、坏 JSON 拒绝、七桶守恒恒等式、resetForTest 归零。
 * 骨架同 ReadRangeToolSkillUriTest（SpillModule.withDefaults + 桩 resolver）。
 */
class ReadRangeStatsTest {

    @TempDir
    Path spillDir;

    private final SkillResourceResolver stubResolver = (sessionId, skillName, relativePath) ->
            "code-review".equals(skillName) && "checklists/security.md".equals(relativePath)
                    ? Optional.of("0123456789abcdef") : Optional.empty();

    @BeforeEach
    void reset() {
        ReadRangeTool.resetForTest();
    }

    private ReadRangeTool toolWith(SkillResourceResolver resolver) {
        SpillModule module = SpillModule.withDefaults(spillDir);
        return new ReadRangeTool(module.service(), resolver);
    }

    @Test
    void skillResourceReadCountsSkillReads() {
        String out = toolWith(stubResolver)
                .call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}");
        assertThat(out).isEqualTo("0123456789abcdef");

        ReadRangeTool.ReadRangeStats stats = ReadRangeTool.stats();
        assertThat(stats.calls()).isEqualTo(1);
        assertThat(stats.skillReads()).isEqualTo(1);
        assertThat(stats.reads()).isZero();
    }

    @Test
    void truncatedSpillReadCountsTruncatedBucket() {
        ReadRangeTool tool = toolWith(stubResolver);
        tool.call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\",\"offset\":4,\"limit\":4}");
        assertThat(ReadRangeTool.stats().skillReads()).isEqualTo(1);
    }

    @Test
    void malformedJsonCountsParseBucket() {
        String out = toolWith(null).call("{not-json");
        assertThat(out).contains("调用失败");

        assertThat(ReadRangeTool.stats().parseRejects()).isEqualTo(1);
        assertThat(ReadRangeTool.stats().failures()).isZero();
    }

    @Test
    void skillModeAndUnwiredCountSkillRejects() {
        ReadRangeTool unwired = toolWith(null);
        assertThat(unwired.call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"json\"}"))
                .contains("仅支持 bytes 模式");
        assertThat(unwired.call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}"))
                .contains("未接线");

        assertThat(ReadRangeTool.stats().skillRejects()).isEqualTo(2);
    }

    @Test
    void unknownSpillPathCountsFailures() {
        // 非 spill 文件的 readBack 异常路径 → failures 兜底桶
        String out = toolWith(null).call("{\"path\":\"spill://ghost\",\"mode\":\"bytes\"}");
        assertThat(out).contains("调用失败");

        assertThat(ReadRangeTool.stats().failures()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        ReadRangeTool tool = toolWith(stubResolver);
        tool.call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"bytes\"}"); // skillReads
        tool.call("{\"path\":\"skill://code-review/nope.md\",\"mode\":\"bytes\"}");               // skillReads（miss 文本亦处理）
        tool.call("{not-json");                                                                    // parse 拒
        tool.call("{\"path\":\"skill://code-review/checklists/security.md\",\"mode\":\"json\"}"); // skill 拒

        ReadRangeTool.ReadRangeStats stats = ReadRangeTool.stats();
        assertThat(stats.calls()).isEqualTo(4);
        assertThat(stats.calls())
                .isEqualTo(stats.reads() + stats.truncatedReads() + stats.skillReads()
                        + stats.parseRejects() + stats.skillRejects() + stats.failures());
        assertThat(stats.skillReads()).isEqualTo(2);
        assertThat(stats.parseRejects()).isEqualTo(1);
        assertThat(stats.skillRejects()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        toolWith(stubResolver).call("{not-json");
        assertThat(ReadRangeTool.stats().calls()).isEqualTo(1);

        ReadRangeTool.resetForTest();

        ReadRangeTool.ReadRangeStats stats = ReadRangeTool.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.parseRejects()).isZero();
        assertThat(stats.failures()).isZero();
    }
}
