package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.ArrayList;
import java.util.List;

/**
 * 语言感知切片器（wayfinder2 impl-19 / T47 / docs/spec/12 §spill-19，采纳源
 * LangChain RecursiveCharacterTextSplitter 144,172★ + aider「先切再解析」纪律）：
 *
 * <ul>
 *   <li><b>Java</b>：花括深度 0 边界 + 成员声明行对齐的启发式「AST-lite」切点
 *       （零依赖；JavaParser 全 AST 为后续可选——非达标源依赖 6.1K★，工程注记不引入）；</li>
 *   <li><b>其他语言/文本</b>：语言分隔符阶梯（递归二分至 maxChars 内）；</li>
 *   <li><b>先切再解析</b>：整文件按安全边界先切段再进一步处理（避 32KB 解析 cliff）；</li>
 *   <li><b>永不静默</b>：每片携带 offset/length 与序号元数据；超长行硬截并显式标记。</li>
 * </ul>
 */
public final class ContentSlicer {

    /** 切片：原文偏移 + 长度（substring(offset, offset+length) 即原文）。 */
    public record Slice(int offset, int length, String text, String marker) {
    }

    private static final int LONG_LINE_CAP = 2_000;

    private ContentSlicer() {
    }

    /** 按语言感知边界切片（maxChars 目标上限；marker 置空 = 无省略）。 */
    public static List<Slice> slice(String content, String language, int maxChars) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        int cap = Math.max(200, maxChars);
        List<int[]> boundaries = "java".equalsIgnoreCase(language == null ? "" : language)
                ? javaBoundaries(content)
                : recursiveBoundaries(content, delimitersFor(language), cap);
        List<Slice> slices = new ArrayList<>();
        int total = boundaries.size();
        for (int i = 0; i < total; i++) {
            int[] boundary = boundaries.get(i);
            String text = content.substring(boundary[0], boundary[1]);
            String marker = "[切片 " + (i + 1) + "/" + total + " offset=" + boundary[0]
                    + " length=" + text.length() + "；完整原文以 spill 句柄可回取]";
            slices.add(new Slice(boundary[0], text.length(), hardCapLongLines(text), marker));
        }
        return slices;
    }

    /** Java「AST-lite」边界：花括深度回到 0 的行尾、成员/类声明行首。 */
    static List<int[]> javaBoundaries(String content) {
        List<Integer> cuts = new ArrayList<>();
        cuts.add(0);
        int depth = 0;
        boolean inString = false;
        boolean inLineComment = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            switch (c) {
                case '/' -> {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '/') {
                        inLineComment = true;
                    }
                }
                case '"' -> inString = true;
                case '{' -> depth++;
                case '}' -> {
                    depth = Math.max(0, depth - 1);
                    if (depth == 0) {
                        int lineEnd = content.indexOf('\n', i);
                        cuts.add(lineEnd < 0 ? content.length() : lineEnd + 1);
                    }
                }
                default -> {
                }
            }
        }
        cuts.add(content.length());
        return toBoundaries(content, cuts.stream().distinct().sorted().toList());
    }

    /** 语言分隔符阶梯的递归二分（LangChain 递归切分语义）。 */
    static List<int[]> recursiveBoundaries(String content, List<String> delimiters, int cap) {
        List<int[]> result = new ArrayList<>();
        split(content, 0, content.length(), delimiters, 0, cap, result);
        return result;
    }

    private static void split(String content, int start, int end, List<String> delimiters,
                             int level, int cap, List<int[]> out) {
        if (end - start <= cap || level >= delimiters.size()) {
            if (end > start) {
                out.add(new int[]{start, end});
            }
            return;
        }
        String delimiter = delimiters.get(level);
        int cursor = start;
        int delimiterAt;
        while ((delimiterAt = content.indexOf(delimiter, cursor)) >= 0
                && delimiterAt + delimiter.length() <= end) {
            int pieceEnd = delimiterAt + delimiter.length();
            split(content, cursor, pieceEnd, delimiters, level + 1, cap, out);
            cursor = pieceEnd;
        }
        if (cursor < end) {
            split(content, cursor, end, delimiters, level + 1, cap, out);
        }
    }

    private static List<int[]> toBoundaries(String content, List<Integer> cuts) {
        List<int[]> boundaries = new ArrayList<>();
        for (int i = 0; i + 1 < cuts.size(); i++) {
            int from = cuts.get(i);
            int to = cuts.get(i + 1);
            if (to > from && !content.substring(from, to).isBlank()) {
                boundaries.add(new int[]{from, to});
            }
        }
        return boundaries;
    }

    private static List<String> delimitersFor(String language) {
        String normalized = language == null ? "" : language.toLowerCase();
        return switch (normalized) {
            case "python" -> List.of("\ndef ", "\nclass ", "\n\n", "\n", ". ", " ");
            case "javascript", "typescript", "js", "ts" ->
                    List.of("\nfunction ", "\nconst ", "\nclass ", "\n}\n", "\n\n", "\n", "; ", " ");
            case "sql" -> List.of(";\n", ";\n\n", "\n\n", "\n", " ");
            case "json" -> List.of("},", "{", ",", " ");
            default -> List.of("\n\n", "\n", ". ", "；", "。", " ");
        };
    }

    /** 超长行硬截 + 显式标记（永不静默：每 LONG_LINE_CAP 段尾注记）。 */
    private static String hardCapLongLines(String text) {
        if (!text.lines().anyMatch(line -> line.length() > LONG_LINE_CAP)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (String line : text.split("\n", -1)) {
            if (line.length() <= LONG_LINE_CAP) {
                out.append(line).append('\n');
                continue;
            }
            for (int start = 0; start < line.length(); start += LONG_LINE_CAP) {
                int end = Math.min(line.length(), start + LONG_LINE_CAP);
                out.append(line, start, end);
                if (end < line.length()) {
                    out.append("[行超长已截断 offset+").append(end - start).append(']');
                }
                out.append('\n');
            }
        }
        return out.toString();
    }
}
