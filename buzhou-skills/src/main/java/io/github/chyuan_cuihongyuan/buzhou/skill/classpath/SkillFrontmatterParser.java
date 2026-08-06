package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SKILL.md frontmatter 解析器（spec 04）。
 *
 * <p>处理对齐 Claude Code 的受限 YAML 子集：{@code ---} 包裹的 frontmatter 块内仅
 * {@code key: value} 行；{@code allowed-tools} 值为逗号分隔列表。不引入完整 YAML 依赖，
 * 避免给内核新增 snakeyaml/jackson-dataformat-yaml。
 */
final class SkillFrontmatterParser {

    private static final String FENCE = "---";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ALLOWED_TOOLS = "allowed-tools";

    private SkillFrontmatterParser() {
    }

    static ParsedSkillMd parse(String content) {
        if (content == null || content.isBlank()) {
            return new ParsedSkillMd(SkillFrontmatter.EMPTY, "");
        }
        String[] lines = content.split("\r?\n", -1);
        if (lines.length == 0 || !FENCE.equals(lines[0].trim())) {
            // 无 frontmatter：整段当正文，无 name（调用方据目录名兜底）
            return new ParsedSkillMd(SkillFrontmatter.EMPTY, content.strip());
        }
        // 找闭合 fence
        int close = -1;
        for (int i = 1; i < lines.length; i++) {
            if (FENCE.equals(lines[i].trim())) {
                close = i;
                break;
            }
        }
        if (close < 0) {
            return new ParsedSkillMd(SkillFrontmatter.EMPTY, content.strip());
        }
        String name = "";
        String description = "";
        List<String> allowedTools = new ArrayList<>();
        for (int i = 1; i < close; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = normalizeKey(line.substring(0, colon));
            String value = line.substring(colon + 1).trim();
            if (value.length() >= 2 && startsAndEndsWithQuote(value)) {
                value = value.substring(1, value.length() - 1);
            }
            switch (key) {
                case KEY_NAME -> name = value;
                case KEY_DESCRIPTION -> description = value;
                case KEY_ALLOWED_TOOLS -> allowedTools.addAll(splitList(value));
                default -> { /* 忽略未知键：向前兼容 */ }
            }
        }
        String body = String.join("\n", Arrays.copyOfRange(lines, close + 1, lines.length)).strip();
        return new ParsedSkillMd(new SkillFrontmatter(name, description, List.copyOf(allowedTools)), body);
    }

    private static String normalizeKey(String raw) {
        return raw.trim().toLowerCase().replace('_', '-');
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static boolean startsAndEndsWithQuote(String value) {
        char q = value.charAt(0);
        return (q == '"' || q == '\'') && value.charAt(value.length() - 1) == q;
    }
}
