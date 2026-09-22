package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

/**
 * 分块压缩策略（spec 1905 / T3011 / impl 1506）——TimescaleDB
 * chunk 压缩语义：历史块（年龄 ≥ compress_after）不可变、压缩率高
 * ——压空间；近期块保持原样——不伤写路径。读路径透明解压，代价
 * = 压缩比（读放大诚实面）。
 *
 * <p>纯函数零状态；压缩执行归存储层。
 */
public final class ChunkCompressionPolicy {

    private ChunkCompressionPolicy() {
    }

    /**
     * 压缩判定：块龄 ≥ compress_after 即压缩（边界含上——恰到期
     * 即可压）。契约：age/compressAfter ≥ 0（fail-fast）。
     */
    public static boolean shouldCompress(long chunkAgeMillis,
                                         long compressAfterMillis) {
        if (chunkAgeMillis < 0) {
            throw new IllegalArgumentException("块龄不能为负：" + chunkAgeMillis);
        }
        if (compressAfterMillis < 0) {
            throw new IllegalArgumentException(
                    "compressAfter 不能为负：" + compressAfterMillis);
        }
        return chunkAgeMillis >= compressAfterMillis;
    }

    /**
     * 空间节省估计：original×(1 − 1/ratio)——压缩比 3:1 省 2/3。
     * 契约：original ≥ 0、ratio > 1（≤ 1 压缩无意义，fail-fast）。
     */
    public static long savingsEstimate(long originalBytes, double compressionRatio) {
        if (originalBytes < 0) {
            throw new IllegalArgumentException(
                    "originalBytes 不能为负：" + originalBytes);
        }
        if (compressionRatio <= 1.0) {
            throw new IllegalArgumentException(
                    "压缩比须 > 1（否则压缩无意义）：" + compressionRatio);
        }
        return (long) (originalBytes * (1.0 - 1.0 / compressionRatio));
    }

    /**
     * 读放大代价：= 压缩比——压缩块查询解压的诚实代价预期。
     */
    public static double readPenaltyFactor(double compressionRatio) {
        if (compressionRatio <= 1.0) {
            throw new IllegalArgumentException(
                    "压缩比须 > 1（否则压缩无意义）：" + compressionRatio);
        }
        return compressionRatio;
    }
}
