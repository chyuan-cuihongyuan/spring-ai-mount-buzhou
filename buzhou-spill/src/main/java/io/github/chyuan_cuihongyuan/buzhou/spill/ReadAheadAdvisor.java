package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.List;

/**
 * 顺序读预读顾问（spec 1818 / T2837 / impl 1419）——Linux readahead 思想：
 * 检测到顺序访问模式即**指数放大预读窗**（块 → 2 块 → 4 块，封顶），随机
 * 访问即**关预读**（读了也白读还挤缓存）。顺序性检测 = 相邻读事件首尾
 * 相接（prev.offset + prev.length == next.offset）构成链。映射到 spill
 * RangeRead：顺序扫描大 handle 时预读翻倍、随机点查时零预读——磁盘往返
 * 次数自适应访问形状。
 *
 * <p>纯函数零状态、只建议不执行（预读动作归宿主）；事件窗口径由调用方
 * 声明（有界，防长历史稀释近况）。
 */
public final class ReadAheadAdvisor {

    /** 预读增长因子上限（2^3 = 8 倍块——Linux max readahead 同义封顶）。 */
    public static final int MAX_GROWTH_FACTOR_EXPONENT = 3;

    /** 构成顺序判定的最小链长（链长 1 = 孤立读，无模式可判）。 */
    public static final int MIN_CHAIN_FOR_PATTERN = 2;

    private ReadAheadAdvisor() {
    }

    /** 单读事件契约：length ≥ 1（零长读不是读）。 */
    public record ReadEvent(long offset, long length) {

        public ReadEvent {
            if (length < 1) {
                throw new IllegalArgumentException("length 不能小于 1：" + length);
            }
        }
    }

    /** 访问形状三态：SEQUENTIAL 顺序链 / RANDOM 跳读 / COLD 样本不足。 */
    public enum Pattern {

        /** 相邻读首尾相接成链——预读翻倍划算。 */
        SEQUENTIAL,

        /** 相邻读跳跃——预读白读。 */
        RANDOM,

        /** 样本不足（< {@link #MIN_CHAIN_FOR_PATTERN} 个事件）——无模式可判。 */
        COLD
    }

    /**
     * @param pattern      判定形状
     * @param readAheadBytes 建议预读字节数（SEQUENTIAL 为 blockSize×2^指数，
     *                       其他 0）
     * @param chainLength  当前顺序链长（非顺序 0）
     */
    public record Advisory(Pattern pattern, long readAheadBytes, long chainLength) {
    }

    /**
     * 顾问入口。契约：blockSize ≥ 1（fail-fast）；null 按空表。
     * 语义：取**最长尾链**（最近连续相接的读）判形状；SEQUENTIAL 的预读
     * = blockSize × 2^min(链长−1, 3)。
     */
    public static Advisory advise(long blockSize, List<ReadEvent> recent) {
        if (blockSize < 1) {
            throw new IllegalArgumentException("blockSize 不能小于 1：" + blockSize);
        }
        List<ReadEvent> window = recent == null ? List.of() : recent;
        if (window.size() < MIN_CHAIN_FOR_PATTERN) {
            return new Advisory(Pattern.COLD, 0, 0);
        }
        // 从尾往前数相接链长
        long chain = 1;
        for (int i = window.size() - 1; i > 0; i--) {
            ReadEvent prev = window.get(i - 1);
            ReadEvent curr = window.get(i);
            if (prev.offset() + prev.length() == curr.offset()) {
                chain++;
            } else {
                break;
            }
        }
        if (chain >= MIN_CHAIN_FOR_PATTERN) {
            int exponent = (int) Math.min(chain - 1, MAX_GROWTH_FACTOR_EXPONENT);
            return new Advisory(Pattern.SEQUENTIAL, blockSize * (1L << exponent), chain);
        }
        return new Advisory(Pattern.RANDOM, 0, 0);
    }
}
