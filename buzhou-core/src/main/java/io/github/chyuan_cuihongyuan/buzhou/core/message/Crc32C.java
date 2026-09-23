package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * CRC-32C 校验（spec 4007 / T6015 / impl 2108）——Castagnoli 多项
 * 式 0x1EDC6F41 表驱动思想（iSCSI/ ext4/LevelDB SSTable/硬件 SSE4.2
 * 同款）：反射算法逐字节查表（crc = (crc&gt;&gt;&gt;8) ^ table[(crc^b)&amp;0xFF]，
 * init/xorout 全 1），突发错误检测强度优于 IEEE CRC-32（关键汉明距
 * 更优）且现代 CPU 有硬件加速路径——**本件为软件基准口径**（同输入
 * 同输出跨平台可对账，硬件路径的结果等价性锚点）。
 *
 * <p>传输/落盘完整性面：与 VarintCodec/EliasGammaCodec 等编码件
 * 成对——编码管紧凑、校验管到达一致。纯静态。
 */
public final class Crc32C {

    /** Castagnoli 反射多项式（CRC-32C 标准）。 */
    private static final long POLY = 0x82F63B78L;   // 0x1EDC6F41 反射形

    private static final long[] TABLE = new long[256];

    static {
        for (int i = 0; i < 256; i++) {
            long c = i;
            for (int k = 0; k < 8; k++) {
                c = (c & 1) != 0 ? (c >>> 1) ^ POLY : c >>> 1;
            }
            TABLE[i] = c;
        }
    }

    private Crc32C() {
    }

    /** 全量校验（32 位值以 long 面——高 32 位恒 0）。 */
    public static long compute(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data 非 null");
        }
        return compute(data, 0, data.length);
    }

    /** 区间校验（off/len 越界 fail-fast）。 */
    public static long compute(byte[] data, int off, int len) {
        if (data == null || off < 0 || len < 0 || off + len > data.length) {
            throw new IllegalArgumentException("data 非 null / 区间合法（off=" + off + " len=" + len + "）");
        }
        long crc = 0xFFFF_FFFFL;
        for (int i = 0; i < len; i++) {
            crc = (crc >>> 8) ^ TABLE[(int) ((crc ^ data[off + i]) & 0xFF)];
        }
        return crc ^ 0xFFFF_FFFFL;
    }

    /** 到达校验（expected 相等即真——传输/落盘一致性口径）。 */
    public static boolean verify(byte[] data, long expected) {
        return compute(data) == expected;
    }
}
