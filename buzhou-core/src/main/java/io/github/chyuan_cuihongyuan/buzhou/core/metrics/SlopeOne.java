package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Slope One 协同过滤（spec 11022 / Y11045 / impl 2475）——Lemire–Maclachlan
 * 2005 思想（Slope One 同源）：**线性偏离 dev_ji=mean(r_kj−r_ki)（共评者域）
 * + 加权预测 (Σ(dev_ji+r_uj)·freq_ji)/Σfreq_ji**——评分矩阵的最简协同过滤
 * 预测面。NaN=未评口径；已评项直读原值（恒等面）。
 *
 * <p>用户/物品越界、无共评基座 fail-fast；null/锯齿行 fail-fast；复算确定。
 */
public final class SlopeOne {

    private SlopeOne() {
    }

    /**
     * 预测用户对物品的评分。
     *
     * @param ratings 评分矩阵（NaN=未评）
     * @param user 用户行
     * @param item 物品列
     * @throws IllegalArgumentException null/锯齿行/越界/该列无共评基座
     */
    public static double predict(double[][] ratings, int user, int item) {
        validate(ratings, user, item);
        double rated = readIfRated(ratings, user, item);
        if (!Double.isNaN(rated)) {
            return rated;
        }
        double weightedSum = 0.0;
        long totalFrequency = 0;
        for (int other = 0; other < ratings[0].length; other++) {
            if (other == item || Double.isNaN(ratings[user][other])) {
                continue;
            }
            long frequency = 0;
            double deviationSum = 0.0;
            for (int rater = 0; rater < ratings.length; rater++) {
                if (rater == user || Double.isNaN(ratings[rater][other])
                        || Double.isNaN(ratings[rater][item])) {
                    continue;
                }
                deviationSum += ratings[rater][other] - ratings[rater][item];
                frequency++;
            }
            if (frequency > 0) {
                double deviation = deviationSum / frequency;
                weightedSum += (deviation + ratings[user][other]) * frequency;
                totalFrequency += frequency;
            }
        }
        if (totalFrequency == 0) {
            throw new IllegalArgumentException("无共评基座（user=" + user
                    + " item=" + item + "）");
        }
        return weightedSum / totalFrequency;
    }

    private static void validate(double[][] ratings, int user, int item) {
        if (ratings == null || ratings.length == 0) {
            throw new IllegalArgumentException("评分矩阵非空");
        }
        int columns = ratings[0].length;
        for (int i = 0; i < ratings.length; i++) {
            if (ratings[i] == null || ratings[i].length != columns) {
                throw new IllegalArgumentException("等长评分行（第 " + i + " 行）");
            }
        }
        if (user < 0 || user >= ratings.length) {
            throw new IllegalArgumentException("用户越界（user=" + user
                    + "，行数 " + ratings.length + "）");
        }
        if (item < 0 || item >= columns) {
            throw new IllegalArgumentException("物品越界（item=" + item
                    + "，列数 " + columns + "）");
        }
    }

    private static double readIfRated(double[][] ratings, int user, int item) {
        return Double.isNaN(ratings[user][item]) ? Double.NaN : ratings[user][item];
    }
}
