package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import java.util.ArrayList;
import java.util.List;

/**
 * Content-Defined Chunking 内容定义分块（spec 5033 / T6167 /
 * impl 2184）——restic/rclone FastCDC 思想：Gear 滚动哈希
 * （64 位窗口无显式滑窗——旧字节自然移出）+ 位掩码边界判定
 * （哈希低位全零即切）+ FastCDC 归一化双掩码（小块区用
 * S 掩码、超过归一化点后用更难命中的 L 掩码——切点偏向
 * 变大、块长方差收窄）；硬上限 maxSize 强切、minSize 内
 * 不切——**块边界只由内容决定**：插入/修改只影响相邻块，
 * 其余块原样可去重（固定字节序列固定切分——确定性可回放）。
 * Gear 表由 SplitMix64 种子生成（无随机、跨进程同表）。
 *
 * <p>与 SegmentLog（spec 5028）同族不同面：字节流切块去重面
 * vs 追加日志保留面。
 */
public final class ContentDefinedChunking {

    /** 一块内容切片（offset/length 覆盖连续不重叠）。 */
    public record Chunk(int offset, int length) {
        public Chunk {
            if (offset < 0 || length <= 0) {
                throw new IllegalArgumentException("offset≥0 且 length>0：" + offset + "/" + length);
            }
        }
    }

    /** Gear 表条目数（单字节索引全表）。 */
    private static final int GEAR_TABLE_SIZE = 256;

    /** 字节无符号掩码（Java byte 有符号——索引取无符号）。 */
    private static final int UNSIGNED_BYTE_MASK = 0xFF;

    /** SplitMix64 黄金常量（种子化 Gear 表生成）。 */
    private static final long SPLITMIX_GAMMA = 0x9e3779b97f4a7c15L;

    /** SplitMix64 混合乘子一。 */
    private static final long SPLITMIX_MULT_A = 0xbf58476d1ce4e5b9L;

    /** SplitMix64 混合乘子二。 */
    private static final long SPLITMIX_MULT_B = 0x94d049bb133111ebL;

    /** 掩码位宽上界（long 语义内安全）。 */
    private static final int MAX_MASK_BITS = 40;

    private final long[] gearTable = new long[GEAR_TABLE_SIZE];
    private final int minSize;
    private final int maxSize;
    private final int normalizationPoint;
    private final long maskSmall;
    private final long maskLarge;

    /**
     * 定构（gearSeed 种子化 Gear 表；minSize 内不切、maxSize
     * 硬上限；avgShiftBits=S 掩码位宽、L 掩码=S+1——FastCDC
     * 归一化；参数畸形 fail-fast）。
     */
    public ContentDefinedChunking(long gearSeed, int minSize, int maxSize, int avgShiftBits) {
        if (minSize < 1 || maxSize <= minSize) {
            throw new IllegalArgumentException("minSize≥1 且 maxSize>minSize："
                    + minSize + "/" + maxSize);
        }
        if (avgShiftBits < 1 || avgShiftBits > MAX_MASK_BITS) {
            throw new IllegalArgumentException("avgShiftBits 1.." + MAX_MASK_BITS + "：" + avgShiftBits);
        }
        long state = gearSeed;
        for (int i = 0; i < GEAR_TABLE_SIZE; i++) {
            state += SPLITMIX_GAMMA;
            long z = state;
            z = (z ^ (z >>> 30)) * SPLITMIX_MULT_A;
            z = (z ^ (z >>> 27)) * SPLITMIX_MULT_B;
            gearTable[i] = z ^ (z >>> 31);
        }
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.normalizationPoint = minSize + (maxSize - minSize) / 2;
        this.maskSmall = (1L << avgShiftBits) - 1;
        this.maskLarge = (1L << (avgShiftBits + 1)) - 1;
    }

    /** 全量切块（边界只由内容决定；覆盖连续不重叠、拼接还原全量）。 */
    public List<Chunk> chunk(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data 非空");
        }
        List<Chunk> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int remaining = data.length - offset;
            long hash = 0;
            int length = 0;
            int cut = remaining;
            while (length < remaining) {
                hash = (hash << 1) + gearTable[data[offset + length] & UNSIGNED_BYTE_MASK];
                length++;
                if (length >= maxSize) {
                    cut = length;
                    break;
                }
                if (length < minSize) {
                    continue;
                }
                long mask = length < normalizationPoint ? maskSmall : maskLarge;
                if ((hash & mask) == 0) {
                    cut = length;
                    break;
                }
            }
            chunks.add(new Chunk(offset, cut));
            offset += cut;
        }
        return List.copyOf(chunks);
    }
}
