package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * 评估数据集 CSV 互操作（spec 527 / T803，LangSmith/HuggingFace datasets
 * CSV 形态）：RFC 4180 解析/序列化（引号包裹、双引号转义、CRLF 兼容）——
 * 数据集与表格工具双向搬运。列固定 header {@code input,expected}（id 由
 * store 添加时重排，非 CSV 关心面）。
 *
 * <p>诚实边界：只搬运 input/expected 两列（溯源/元数据列不入 CSV——
 * 最小互操作面）；解析为纯内存行表。
 */
public final class EvalDatasetCsv {

    /** CSV 固定表头。 */
    public static final String HEADER = "input,expected";

    private EvalDatasetCsv() {
    }

    /** 序列化：表头 + 每行 RFC 4180 转义（含逗号/引号/换行的字段自动包裹）。 */
    public static String toCsv(List<EvalItem> items) {
        if (items == null) {
            throw new IllegalArgumentException("items 必须非空");
        }
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (EvalItem item : items) {
            sb.append(escape(item.input())).append(',')
                    .append(escape(item.expected())).append('\n');
        }
        return sb.toString();
    }

    /** 解析：首行表头校验（宽松：容忍大小写与空白），数据行 → 候选项（id 空）。 */
    public static List<EvalItem> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV 非空");
        }
        List<String[]> rows = parse(csv);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 缺表头行");
        }
        String[] header = rows.getFirst();
        if (header.length < 2 || !header[0].trim().equalsIgnoreCase("input")
                || !header[1].trim().equalsIgnoreCase("expected")) {
            throw new IllegalArgumentException("CSV 表头须为 input,expected（实际 "
                    + String.join("|", header) + "）");
        }
        List<EvalItem> items = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 2) {
                continue; // 空行容忍
            }
            items.add(new EvalItem(null, row[0], row[1], null, null, null));
        }
        return items;
    }

    /** Writer 形态导出（60/67 导出族同构）。 */
    public static long export(Writer out, List<EvalItem> items) throws IOException {
        out.write(toCsv(items));
        return items.size();
    }

    // ---- RFC 4180 ----

    private static String escape(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    /** RFC 4180 状态机解析（引号域内逗号/换行/CRLF 兼容；返回行表）。 */
    private static List<String[]> parse(String csv) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < csv.length()) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
            i++;
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }
}
