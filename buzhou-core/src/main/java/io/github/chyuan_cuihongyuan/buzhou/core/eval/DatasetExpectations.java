package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 数据集期望套件（spec 134 §A / T459，Great Expectations 借鉴）：run 前
 * 对数据集行面做声明式校验——脏数据（空输入/重复行/规模异常）在进评估器前
 * fail-fast，而不是以畸形分数的形式晚暴露。
 *
 * <p><b>口径</b>：只读校验（不修数）；发现项有界（每期望默认 {@value
 * #MAX_FINDINGS_PER_EXPECTATION} 条样本 + 计数，防止脏数据集把报告本身打爆）；
 * 期望名单行 summary——CI 单行可读。内置期望：非空输入 / 期望值在场 /
 * 输入唯一 / 规模窗口；自定义期望经 {@link #named(String, Predicate)}（名单
 * 纪律：空白拒绝）。
 */
public final class DatasetExpectations {

    /** 每期望发现样本封顶（报告面有界）。 */
    public static final int MAX_FINDINGS_PER_EXPECTATION = 10;

    /** 单条发现（期望名 + 行号 -1 表全数据集级 + 有界明细）。 */
    public record Finding(String expectation, int itemIndex, String detail) {
    }

    /** 校验结果（通过 = findings 空；summary 单行）。 */
    public record Result(boolean passed, List<Finding> findings, int itemCount) {

        /** CI 单行摘要（通过/失败 + 期望名去重清单）。 */
        public String summary() {
            if (passed) {
                return "dataset expectations: PASS (" + itemCount + " items)";
            }
            Set<String> names = new HashSet<>();
            findings.forEach(f -> names.add(f.expectation()));
            return "dataset expectations: FAIL (" + itemCount + " items, violated="
                    + String.join(",", names.stream().sorted().toList()) + ")";
        }
    }

    /** 单条期望（名单 + 行级谓词；全数据集级期望在谓词内看全表）。 */
    public record Expectation(String name, Predicate<EvalItem> rowPredicate,
                              Predicate<List<EvalItem>> datasetPredicate) {

        static Expectation row(String name, Predicate<EvalItem> predicate) {
            return new Expectation(name, predicate, null);
        }

        static Expectation dataset(String name, Predicate<List<EvalItem>> predicate) {
            return new Expectation(name, null, predicate);
        }
    }

    private final List<Expectation> expectations;

    private DatasetExpectations(List<Expectation> expectations) {
        this.expectations = List.copyOf(expectations);
    }

    /** 空套件（恒过——零期望 = 零意见，诚实）。 */
    public static DatasetExpectations none() {
        return new DatasetExpectations(List.of());
    }

    public static DatasetExpectations of(Expectation... expectations) {
        Objects.requireNonNull(expectations, "expectations");
        for (Expectation expectation : expectations) {
            if (expectation == null || expectation.name() == null
                    || expectation.name().isBlank()) {
                throw new IllegalArgumentException("expectation name must not be blank");
            }
        }
        return new DatasetExpectations(List.of(expectations));
    }

    /** 自定义行级期望（名单非空白）。 */
    public static Expectation named(String name, Predicate<EvalItem> rowPredicate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("expectation name must not be blank");
        }
        Objects.requireNonNull(rowPredicate, "rowPredicate");
        return Expectation.row(name, rowPredicate);
    }

    /** 内置：输入非空白。 */
    public static Expectation nonBlankInputs() {
        return Expectation.row("non-blank-inputs", item -> item.input() != null
                && !item.input().isBlank());
    }

    /** 内置：期望值在场（期望缺失的行让打分口径静默漂移——拒绝）。 */
    public static Expectation expectedPresent() {
        return Expectation.row("expected-present", item -> item.expected() != null
                && !item.expected().isBlank());
    }

    /** 内置：输入唯一（重复行加权指标——除非刻意，否则拒绝）。 */
    public static Expectation uniqueInputs() {
        return Expectation.dataset("unique-inputs", items -> {
            Set<String> seen = new HashSet<>();
            return items.stream().allMatch(item -> seen.add(item.input()));
        });
    }

    /** 内置：规模窗口 [min, max]（空集/巨集都是管线事故的信号）。 */
    public static Expectation sizeBetween(int min, int max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("invalid size window [" + min + ", " + max + "]");
        }
        return Expectation.dataset("size-between-" + min + "-" + max,
                items -> items.size() >= min && items.size() <= max);
    }

    /** 校验（只读；发现样本按期望封顶 + 全量计数经明细前缀「共 N 处」呈现）。 */
    public Result validate(List<EvalItem> items) {
        Objects.requireNonNull(items, "items");
        List<Finding> findings = new ArrayList<>();
        for (Expectation expectation : expectations) {
            int violations = 0;
            List<Finding> samples = new ArrayList<>();
            if (expectation.rowPredicate() != null) {
                for (int i = 0; i < items.size(); i++) {
                    if (!expectation.rowPredicate().test(items.get(i))) {
                        violations++;
                        if (samples.size() < MAX_FINDINGS_PER_EXPECTATION) {
                            samples.add(new Finding(expectation.name(), i,
                                    "input=" + preview(items.get(i).input())));
                        }
                    }
                }
            } else if (!expectation.datasetPredicate().test(items)) {
                // 数据集级：单条发现自带 itemCount——无需样本封顶/溢出行
                samples.add(new Finding(expectation.name(), -1,
                        "itemCount=" + items.size()));
            }
            findings.addAll(samples);
            if (violations > samples.size()) {
                findings.add(new Finding(expectation.name(), -1,
                        "共 " + violations + " 处（样本封顶 " + MAX_FINDINGS_PER_EXPECTATION + "）"));
            }
        }
        return new Result(findings.isEmpty(), List.copyOf(findings), items.size());
    }

    private static String preview(String input) {
        String oneLine = input == null ? "<null>"
                : input.lines().findFirst().orElse("");
        return oneLine.length() > 60 ? oneLine.substring(0, 60) + "…" : oneLine;
    }
}
