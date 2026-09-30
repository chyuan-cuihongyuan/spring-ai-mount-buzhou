package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * SplitMix64 可分裂随机（spec 9039 / W9079 / impl 2392）——Steele
 * 2014 思想（「Lea's splitmix」——JDK 8 java.util.SplittableRandom
 * 内核同源/Libc++ std::splitmix64 同款）：**黄金比例 γ=0x9E97…
 * 定序器：state+=γ 后黄金比例最终化（z^=z>>>30 乘·、z^=z>>>27
 * 乘、z^=z>>>31）**——相邻种子雪崩铺展（种子分裂即独立流）——
 * LCG 相邻种子相关（短程相关缺陷）的病解。Steele 公开金向量
 * 锚（mix(0)=0xe220a8397b1dcdaf、mix(1)=0x6e789e6aa1b965f4）；
 * 状态/输出分离（nextState 读推进 + mixOutput 混合读——可分
 * 裂语义）；确定性纯函数；非密码学明示。
 *
 * <p>与 XorShift64（spec 9038）同族不同面：定序器 vs 推进器
 * （组合搭配成强 RNG——SplittableRandom 内核同源）；与
 * Treap（concurrent 域）消费同源：种子化优先级。
 */
public final class SplitMix64 {

    /** 黄金比例伽马（Steele 2014 定序步进常量）。 */
    public static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private SplitMix64() {
    }

    /**
     * 状态推进（state+γ——分裂点确定性偏移；状态 0 合法，γ 步进即逃逸）。
     */
    public static long nextState(long state) {
        return state + GOLDEN_GAMMA;
    }

    /**
     * 混合输出（黄金比例最终化——定序器核心）。
     *
     * @throws IllegalArgumentException 输入 0 拒绝（混合零恒零）
     */
    public static long mixOutput(long z) {
        if (z == 0) {
            throw new IllegalArgumentException("混合输入非 0（零恒零拒绝）");
        }
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** 一步随机（nextState+mixOutput——流式便利面）。 */
    public static long next(long state) {
        return mixOutput(nextState(state));
    }
}
