package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.ArrayList;
import java.util.List;

/**
 * PII 检测器合成探针自查（spec 1407 / T2115 / impl 1060）——spaCy NER
 * 评测 / Presidio analyze 打分思想：用<b>确定性合成样本</b>（已知正例 +
 * 已知负例）穿测 {@link PiiDetector#scan}，按类型报召回率 + 全局误报数，
 * 检测器回归（规则改动/CustomPiiRules 变更）从「线上漏报后追溯」变成
 * 「例行自查显形」。
 *
 * <p>样本全部为合成构造（非真实个人信息）；正例按 {@link PiiType} 分组、
 * 负例为不含任何 PII 形态的普通文本。纯函数零状态：探针结果即时返回，
 * 不落台账不接 hook（例行化接线留宿主）。
 */
public final class PiiProbeSelfCheck {

    private PiiProbeSelfCheck() {
    }

    /**
     * 每类型合成正例（已知应被检出）。身份证号<b>不入池</b>：CN_RESIDENT_ID 带
     * 校验位验证，构造校验位合法的合成号有撞真实证件号的概率（红线）；检测器
     * 该轴由仓内既有单元测试覆盖。卡号用业界通用测试 PAN（Luhn 合法、非真实卡）。
     */
    private static final List<Probe> POSITIVES = List.of(
            new Probe(PiiType.EMAIL, "probe.alice@example-verify.com"),
            new Probe(PiiType.EMAIL, "bob.test@corp-mail.example.org"),
            new Probe(PiiType.CN_PHONE, "13800138000"),
            new Probe(PiiType.CN_PHONE, "15912345678"),
            new Probe(PiiType.BANK_CARD, "4242424242424242"),
            new Probe(PiiType.IPV4, "10.193.168.24"),
            new Probe(PiiType.IPV4, "172.16.4.7"));

    /** 合成负例（已知不应有任何命中——误报哨兵）。 */
    private static final List<String> NEGATIVES = List.of(
            "今天的会话摘要包含十二个轮次与三次工具调用。",
            "plain text without any sensitive patterns 123 abc",
            "订单号 ORD-2026-0914-0001 已完成出库",
            "版本 v1.2.3 于本周四发布");

    /**
     * @param type    期望检出的 PII 类型
     * @param sample  合成样本文本
     */
    private record Probe(PiiType type, String sample) {
    }

    /** 单类型召回：hits/total（total = 该类型正例数）。 */
    public record TypeRecall(PiiType type, int hits, int total) {

        /** 召回率（total=0 时哨兵 -1，当前枚举下不发生）。 */
        public double recall() {
            return total == 0 ? -1d : (double) hits / total;
        }
    }

    /**
     * @param recalls         逐类型召回（按 PiiType 典序）
     * @param falsePositives  负例中的误报命中总数（应恒为 0——非零即检测器回归）
     */
    public record ProbeReport(List<TypeRecall> recalls, int falsePositives) {

        /** 全局召回率（Σhits/Σtotal）。 */
        public double overallRecall() {
            int hits = 0;
            int total = 0;
            for (TypeRecall r : recalls) {
                hits += r.hits();
                total += r.total();
            }
            return total == 0 ? -1d : (double) hits / total;
        }
    }

    /** 穿测入口：对 detector 跑全部合成正负例。 */
    public static ProbeReport probe(PiiDetector detector) {
        List<TypeRecall> recalls = new ArrayList<>();
        for (PiiType type : PiiType.values()) {
            List<Probe> samples = POSITIVES.stream()
                    .filter(p -> p.type() == type)
                    .toList();
            if (samples.isEmpty()) {
                continue;
            }
            int hits = 0;
            for (Probe probe : samples) {
                if (containsMatch(detector.scan(probe.sample()), type)) {
                    hits++;
                }
            }
            recalls.add(new TypeRecall(type, hits, samples.size()));
        }
        recalls.sort(java.util.Comparator.comparing(TypeRecall::type));
        int falsePositives = 0;
        for (String negative : NEGATIVES) {
            falsePositives += detector.scan(negative).size();
        }
        return new ProbeReport(List.copyOf(recalls), falsePositives);
    }

    private static boolean containsMatch(List<PiiDetector.PiiMatch> matches, PiiType type) {
        return matches.stream().anyMatch(m -> m.type() == type);
    }
}
