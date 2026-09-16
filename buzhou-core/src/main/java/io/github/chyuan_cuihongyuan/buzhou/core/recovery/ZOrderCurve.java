package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * Z 序曲线（spec 2064 / T3229 / impl 1615）——Morton 码 / Z-order
 * 思想：多维坐标位交织为一维标量——**空间相邻的点标量也相近**（每维
 * 交替贡献一位），多维范围查询退化为标量区间/前缀扫描（一维索引直
 * 接服务多维局部性）；两维 int → long 版本（时间 × 租户 / 轮次 ×
 * 工具的多维键压缩）。
 *
 * <p>纯函数零状态、确定性；解码往返（encode → decode == 原值）。
 */
public final class ZOrderCurve {

    /** 各维可编码位数（int 32 位 → long 64 位恰容两维）。 */
    public static final int BITS_PER_DIM = 32;

    /** 二维坐标点。 */
    public record Point(int x, int y) {
    }

    private ZOrderCurve() {
    }

    /**
     * 编码：x 占偶数位、y 占奇数位（x 最低位进 bit0、y 进 bit1，逐位
     * 交替）——相邻坐标的标量差随距离衰减（局部性保持）。
     */
    public static long encode(int x, int y) {
        long result = 0L;
        for (int bit = 0; bit < BITS_PER_DIM; bit++) {
            result |= ((x >>> bit) & 1L) << (2 * bit);       // x → 偶位
            result |= ((y >>> bit) & 1L) << (2 * bit + 1);   // y → 奇位
        }
        return result;
    }

    /** 解码：偶位归 x、奇位归 y——与 encode 互逆。 */
    public static Point decode(long z) {
        int x = 0;
        int y = 0;
        for (int bit = 0; bit < BITS_PER_DIM; bit++) {
            x |= (int) ((z >>> (2 * bit)) & 1L) << bit;
            y |= (int) ((z >>> (2 * bit + 1)) & 1L) << bit;
        }
        return new Point(x, y);
    }

    /**
     * 局部性读数：两点的 Z 序标量差 vs 曼哈顿距离——小邻域（如 8×8
     * 内）标量差与距离同数量级（局部性保持的量化口径；大跨度有 Z 形
     * 跳变——全局局部性不保证，仅邻域）。
     */
    public static long scalarGap(int x1, int y1, int x2, int y2) {
        return Math.abs(encode(x1, y1) - encode(x2, y2));
    }

    /** 曼哈顿距离（局部性对照量）。 */
    public static long manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs((long) x1 - x2) + Math.abs((long) y1 - y2);
    }
}
