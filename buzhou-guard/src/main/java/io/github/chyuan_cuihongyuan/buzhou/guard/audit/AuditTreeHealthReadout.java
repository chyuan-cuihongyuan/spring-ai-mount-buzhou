package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

/**
 * 审计树形健康读数（spec 830 / T1161，Certificate Transparency 树语义扩散；
 * 838 Merkle 树的形状面）：对叶数做树形健康判定——深度/补位叶/满树比——
 * 「审计树是不是矮胖健康（接近 2^n）还是细高畸形」一表可见（细高树证明
 * 路径更长、批量验证更慢的量化依据）。
 *
 * <p>纯函数：leafCount ≥ 0；深度 = ⌈log2(max(leafCount,1))⌉；补位 =
 * nextPow2 − leafCount；满树 = leafCount 为 2 的幂（含 1）。叶数由调用方
 * 自 {@code AuditMerkleTree} 采集（零侵入）。
 */
public final class AuditTreeHealthReadout {

    /** 不可变健康行。 */
    public record TreeHealth(int leafCount, int depth, int nextPowerOfTwo,
                             int paddingLeaves, boolean perfectTree) {
    }

    private AuditTreeHealthReadout() {
    }

    /** 树形健康判定（负数按 0 计——脏入参不炸）。 */
    public static TreeHealth analyze(int leafCount) {
        int leaves = Math.max(0, leafCount);
        if (leaves == 0) {
            return new TreeHealth(0, 0, 1, 1, false);
        }
        // 深度 = ⌈log2(leaves)⌉ = 32 − numberOfLeadingZeros(leaves−1)
        int depth = 32 - Integer.numberOfLeadingZeros(leaves - 1);
        int nextPow2 = 1 << depth;
        boolean perfect = leaves == nextPow2;
        return new TreeHealth(leaves, depth, nextPow2, nextPow2 - leaves, perfect);
    }
}
