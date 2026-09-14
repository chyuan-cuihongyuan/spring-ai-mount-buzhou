package io.github.chyuan_cuihongyuan.buzhou.mcp.internal;

import io.github.chyuan_cuihongyuan.buzhou.mcp.config.BuzhouMcpProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1507 / T2265–T2266：默认动词模式集经 globToRegex 的命中面——
 * 写侧动词命中（含大小写不敏感）、只读动词不误伤（globToRegex 包私有故本测试在 internal 包）。
 */
class McpDefaultDangerousGlobTest {

    @Test
    void defaultPatternsShouldMatchWriteSideVerbsOnly() {
        List<Pattern> compiled = BuzhouMcpProperties.DEFAULT_DANGEROUS_TOOL_PATTERNS
                .stream().map(DefaultMcpClientRegistry::globToRegex).toList();
        assertThat(matchesAny(compiled, "delete_records")).isTrue();
        assertThat(matchesAny(compiled, "exec_sql")).isTrue();
        assertThat(matchesAny(compiled, "send_email")).isTrue();
        assertThat(matchesAny(compiled, "update_user")).isTrue();
        assertThat(matchesAny(compiled, "drop_table")).isTrue();
        assertThat(matchesAny(compiled, "remove_old")).isTrue();
        assertThat(matchesAny(compiled, "WriteFile")).isTrue(); // 大小写不敏感
        // 只读动词不误伤（read/list/get/query/search）
        assertThat(matchesAny(compiled, "read_query")).isFalse();
        assertThat(matchesAny(compiled, "list_items")).isFalse();
        assertThat(matchesAny(compiled, "get_user")).isFalse();
        assertThat(matchesAny(compiled, "search_docs")).isFalse();
    }

    private static boolean matchesAny(List<Pattern> patterns, String toolName) {
        return patterns.stream().anyMatch(p -> p.matcher(toolName).matches());
    }
}
