package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

/**
 * 海明 SECDED 码（spec 8040 / V8079 / impl 2341）——
 * Hamming 1950 思想（贝尔实验室）：**Hamming(8,4)：4 数据
 * 位+3 校验位+整体奇偶位 → 8 码位——单比特纠错、双比特检
 * 错不可纠**——重传代价高（单比特误码就地恢复）的病解。
 * decode 三态（0 无错/1 纠 1 位/2 检 2 位不可纠——数据按
 * 接收值返回）；数据越域 fail-fast；确定性纯函数。
 *
 * <p>与 TotpGenerator（spec 8038）同族不同面：传输纠错面
 * vs 口令生成面。
 */
public final class HammingCode {

    /** decode 三态：无错。 */
    public static final int CLEAN = 0;
    /** decode 三态：纠 1 位。 */
    public static final int CORRECTED = 1;
    /** decode 三态：检 2 位不可纠。 */
    public static final int DOUBLE_ERROR = 2;

    /** 本布局（p1 p2 d1 p3 d2 d3 d4）的综合征→数据位翻转掩码（校验位综合征→0 不动数据——勘误：初版 4−syndrome 映射错位——显式映射钉住修正）。 */
    private static int dataBitForSyndrome(int syndrome) {
        return switch (syndrome) {
            case 6 -> 0x8;
            case 5 -> 0x4;
            case 3 -> 0x2;
            case 7 -> 0x1;
            default -> 0;
        };
    }

    private HammingCode() {
    }

    /** 编码 4 数据位（低 4 位）→ 8 码位（数据越域 fail-fast）。 */
    public static int encode(int data) {
        if (data < 0 || data > 15) {
            throw new IllegalArgumentException("4 数据位 ∈[0,15]（实际 " + data + "）");
        }
        int d1 = (data >> 3) & 1;
        int d2 = (data >> 2) & 1;
        int d3 = (data >> 1) & 1;
        int d4 = data & 1;
        int p1 = d1 ^ d2 ^ d4;
        int p2 = d1 ^ d3 ^ d4;
        int p3 = d2 ^ d3 ^ d4;
        int code = (p1 << 7) | (p2 << 6) | (d1 << 5) | (p3 << 4) | (d2 << 3) | (d3 << 2) | (d4 << 1);
        int parity = Integer.bitCount(code) & 1;
        return code | parity;
    }

    /**
     * 解码 8 码位 → 低 4 位数据（高 4 位=三态：0 无错/1 纠 1 位/2 双错不可纠）。
     */
    public static int decode(int code) {
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException("8 码位 ∈[0,255]（实际 " + code + "）");
        }
        int p1 = (code >> 7) & 1;
        int p2 = (code >> 6) & 1;
        int d1 = (code >> 5) & 1;
        int p3 = (code >> 4) & 1;
        int d2 = (code >> 3) & 1;
        int d3 = (code >> 2) & 1;
        int d4 = (code >> 1) & 1;
        int overall = code & 1;
        int syndrome = (p1 ^ d1 ^ d2 ^ d4) << 2 | (p2 ^ d1 ^ d3 ^ d4) << 1 | (p3 ^ d2 ^ d3 ^ d4);
        int data = (d1 << 3) | (d2 << 2) | (d3 << 1) | d4;
        int totalParity = Integer.bitCount(code) & 1;
        int status;
        if (totalParity == 1) {
            status = CORRECTED;
            data ^= dataBitForSyndrome(syndrome);
        } else if (syndrome != 0) {
            status = DOUBLE_ERROR;
        } else {
            status = CLEAN;
        }
        return (status << 4) | data;
    }
}
