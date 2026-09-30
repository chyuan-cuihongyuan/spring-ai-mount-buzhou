package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.BitSet;
import java.util.List;

/**
 * cron 五域解析（spec 9042 / W9085 / impl 2395）——Vixie cron
 * 思想（BSD cron/Unix 定时任务 50 年事实标准——Quartz/
 * Spring @Scheduled 同源）：**分 时 日 月 周 五域各自展开为
 * 位集，任一时刻五域全命中即触发**——手写 if-else 时刻判断
 * （五维组合爆炸不可维护）的病解。域语法：`*`、数字、区间
 * a-b、步进 * /n 与 a-b/n、列表 a,b,c（组合任意）；周域 0–7
 * （0/7 皆周日）；纯数字域明示（月份/星期名不收——README 契
 * 约）；域界外/语法非法 fail-fast；同表达式同位集完全确定。
 *
 * <p>与 HashedWheelTimers（concurrent 域）同根不同面：到期
 * 触发引擎 vs 触发判定文法；与 DelayScheduling（同包）不同
 * 面：延迟等待 vs 日历语义。
 */
public final class CronFieldParser {

    private static final int[] MAX = {59, 23, 31, 12, 7};

    private final BitSet[] fields;

    private CronFieldParser(BitSet[] fields) {
        this.fields = fields;
    }

    /**
     * 解析五域表达式（空格分隔：分 时 日 月 周）。
     *
     * @throws IllegalArgumentException null/域数≠5/域界越域/语法非法
     */
    public static CronFieldParser parse(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("表达式非空引用");
        }
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("五域表达式（实际 " + parts.length + " 域）");
        }
        BitSet[] parsed = new BitSet[5];
        for (int f = 0; f < 5; f++) {
            parsed[f] = parseField(parts[f], 0, MAX[f]);
        }
        return new CronFieldParser(parsed);
    }

    /** 触发判定（给定时刻五分量全命中）。 */
    public boolean matches(int minute, int hour, int dayOfMonth, int month, int dayOfWeek) {
        int[] moment = {minute, hour, dayOfMonth, month, dayOfWeek};
        for (int f = 0; f < 5; f++) {
            if (moment[f] < 0 || moment[f] > MAX[f]) {
                throw new IllegalArgumentException("时刻分量越域（" + moment[f] + " > " + MAX[f] + "）");
            }
            if (!fields[f].get(moment[f])) {
                return false;
            }
        }
        return true;
    }

    /** 域命中读数（升序值列——测试与可观测面）。 */
    public List<Integer> fieldValues(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= 5) {
            throw new IllegalArgumentException("域序 ∈[0,5)（实际 " + fieldIndex + "）");
        }
        return fields[fieldIndex].stream().boxed().toList();
    }

    private static BitSet parseField(String text, int min, int max) {
        BitSet set = new BitSet(max + 1);
        for (String item : text.split(",")) {
            if (item.isEmpty()) {
                throw new IllegalArgumentException("空列表项（\"" + text + "\"）");
            }
            String rangePart = item;
            int step = 1;
            int slash = item.indexOf('/');
            if (slash >= 0) {
                rangePart = item.substring(0, slash);
                try {
                    step = Integer.parseInt(item.substring(slash + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("步进非整数（\"" + item + "\"）");
                }
                if (step < 1) {
                    throw new IllegalArgumentException("步进为正（\"" + item + "\"）");
                }
            }
            int from;
            int to;
            if (rangePart.equals("*")) {
                from = min;
                to = max;
            } else if (rangePart.contains("-")) {
                String[] bounds = rangePart.split("-", -1);
                if (bounds.length != 2) {
                    throw new IllegalArgumentException("区间双端（\"" + rangePart + "\"）");
                }
                from = parseIntOrSunday(bounds[0], text);
                to = parseIntOrSunday(bounds[1], text);
            } else {
                from = parseIntOrSunday(rangePart, text);
                to = slash >= 0 ? max : from;
            }
            if (from < min || to > max || from > to) {
                throw new IllegalArgumentException("域界 [" + min + "," + max + "]（\"" + item + "\"）");
            }
            for (int v = from; v <= to; v += step) {
                set.set(v);
            }
        }
        if (set.isEmpty()) {
            throw new IllegalArgumentException("域为空（\"" + text + "\"）");
        }
        return set;
    }

    /** 周域特例：7 归一为 0（周日）。 */
    private static int parseIntOrSunday(String token, String field) {
        try {
            int value = Integer.parseInt(token);
            return value == 7 ? 0 : value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非整数（\"" + token + "\" 于 \"" + field + "\"）");
        }
    }
}
