package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1507 / T2265–T2266：MCP 危险工具默认动词模式（design-incompleteness S1
 * 修复）——属性三态语义（缺省=七动词默认集 / 显式空=关闭逃生门 / 非空=透传）+
 * 默认集经 globToRegex 的命中面（写侧动词命中、只读动词不误伤）。
 */
class McpDefaultDangerousPatternsTest {

    /** 缺省（null/未配置）→ 七动词默认集（spec 14 §F 承诺落地）。 */
    @Test
    void absentConfigShouldDefaultToVerbPatterns() {
        BuzhouMcpProperties props = new BuzhouMcpProperties(null, null, null);
        assertThat(props.dangerousToolPatterns())
                .containsExactlyInAnyOrder("delete*", "drop*", "write*", "update*",
                        "remove*", "send*", "exec*");
        assertThat(props.dangerousToolPatterns())
                .isEqualTo(BuzhouMcpProperties.DEFAULT_DANGEROUS_TOOL_PATTERNS);
    }

    /** 显式空列表 = 用户显式关闭（逃生门：yml [] 绑定空 List 非 null）。 */
    @Test
    void explicitEmptyListShouldDisableRegistration() {
        BuzhouMcpProperties props = new BuzhouMcpProperties(null, List.of(), Duration.ofSeconds(35));
        assertThat(props.dangerousToolPatterns()).isEmpty();
    }

    /** 非空显式集透传（不注入默认）。 */
    @Test
    void explicitPatternsShouldPassThroughWithoutDefaults() {
        BuzhouMcpProperties props = new BuzhouMcpProperties(null, List.of("*.destroy*"),
                Duration.ofSeconds(35));
        assertThat(props.dangerousToolPatterns()).containsExactly("*.destroy*");
    }
}
