package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 技能正文规模审计（spec 546 / T839——110 目录预算的 per-skill 深化）：
 * 逐技能正文字符规模 + 预算超限标记（load_skill 载荷膨胀的静态审计面——
 * 正文是 load_skill 的返回载荷，超预算技能每次加载都吃上下文）。
 * 纯函数。
 *
 * <p>诚实边界：只审计正文规模（描述/资源列表不计——目录注入面归 110
 * 遥测）；纯读数不拦截。
 */
public final class SkillBodyAudit {

    /** 单技能审计行。 */
    public record SkillRow(String name, int bodyChars, boolean overBudget) {
    }

    /** 审计报告（rows 按 bodyChars 降序；stats 聚合）。 */
    public record Report(int skills, int totalChars, int maxChars, double avgChars,
            int overBudgetCount, List<SkillRow> rows) {
    }

    private SkillBodyAudit() {
    }

    /** 审计：budgetChars ≤ 0 = 不判超限（只报规模）。 */
    public static Report analyze(List<Skill> skills, int budgetChars) {
        if (skills == null) {
            throw new IllegalArgumentException("skills 必须非空");
        }
        List<SkillRow> rows = new ArrayList<>();
        int total = 0;
        int max = 0;
        int over = 0;
        for (Skill skill : skills) {
            int chars = skill.body() == null ? 0 : skill.body().length();
            total += chars;
            max = Math.max(max, chars);
            boolean overBudgetFlag = budgetChars > 0 && chars > budgetChars;
            if (overBudgetFlag) {
                over++;
            }
            rows.add(new SkillRow(skill.name(), chars, overBudgetFlag));
        }
        rows.sort(Comparator.comparingInt(SkillRow::bodyChars).reversed()
                .thenComparing(SkillRow::name));
        double avg = skills.isEmpty() ? 0.0 : (double) total / skills.size();
        return new Report(skills.size(), total, max, avg, over, List.copyOf(rows));
    }
}
