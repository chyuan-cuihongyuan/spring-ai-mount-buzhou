package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

/**
 * LSM 写放大读面（spec 1914 / T3029 / impl 1515）——RocksDB/LSM
 * write amplification：落盘字节 / 逻辑写入字节。LSM 层级压实把
 * 同一份数据反复重写（正常 WAF 2–10），越界即压实风暴——「盘在为
 * 谁工作」有账，容量排查分得清写多还是重写多。
 *
 * <p>纯函数零状态；压实执行归存储层。
 */
public final class WriteAmplificationFactor {

    private WriteAmplificationFactor() {
    }

    /**
     * 写放大因子：落盘字节/逻辑写入字节。契约：written ≥ 0、
     * logical ≥ 1（零逻辑写入无分母，fail-fast）。
     */
    public static double waf(long bytesWrittenToDisk, long logicalBytesIngested) {
        if (bytesWrittenToDisk < 0) {
            throw new IllegalArgumentException(
                    "落盘字节不能为负：" + bytesWrittenToDisk);
        }
        if (logicalBytesIngested < 1) {
            throw new IllegalArgumentException(
                    "逻辑写入字节不能小于 1：" + logicalBytesIngested);
        }
        return (double) bytesWrittenToDisk / logicalBytesIngested;
    }

    /**
     * 压实债读数：待压实字节/盘容量——越高越逼近强制停写。契约：
     * pending ≥ 0、capacity ≥ 1（fail-fast）。
     */
    public static double compactionDebtRatio(long pendingCompactionBytes,
                                             long diskCapacityBytes) {
        if (pendingCompactionBytes < 0) {
            throw new IllegalArgumentException(
                    "待压实字节不能为负：" + pendingCompactionBytes);
        }
        if (diskCapacityBytes < 1) {
            throw new IllegalArgumentException(
                    "盘容量不能小于 1：" + diskCapacityBytes);
        }
        return (double) pendingCompactionBytes / diskCapacityBytes;
    }

    /**
     * 限速建议：WAF ≥ threshold 即建议写入限速（压实风暴前置信号）。
     * 契约：threshold ≥ 1（fail-fast）。
     */
    public static boolean needsThrottle(double waf, double threshold) {
        if (threshold < 1.0) {
            throw new IllegalArgumentException(
                    "threshold 不能小于 1：" + threshold);
        }
        return waf >= threshold;
    }
}
