package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Random;

/**
 * Zobrist 哈希（spec 7034 / U7269 / impl 2286）——Zobrist
 * 1970 思想（棋类引擎/图同构测试同源）：**每 (位置,种类)
 * 一张随机长码表，状态哈希=命中码异或和**——增量更新
 * O(1)（走子 xorIn/xorOut 单表项异或）——全状态重算 O(位置)
 * 与全序比较（棋谱等价判定）的病解。空位（−1）贡献 0
 * ——异或交换律保证更新顺序无关。种子化表（同种子同表
 * ——确定性可回放；碰撞概率诚实边界：64 位随机——非
 * 加密承诺）。
 *
 * <p>与 XxHash64（message）同族不同面：整块字节流 vs
 * 增量可异或的表驱动。
 */
public final class ZobristHashing {

    private final long[][] table;

    /** positions≥1、kinds≥1、种子注入（越域 fail-fast）。 */
    public ZobristHashing(int positions, int kinds, long seed) {
        if (positions < 1 || kinds < 1) {
            throw new IllegalArgumentException("位置与种类须为正: "
                    + positions + "x" + kinds);
        }
        this.table = new long[positions][kinds];
        Random rng = new Random(seed);
        for (int i = 0; i < positions; i++) {
            for (int k = 0; k < kinds; k++) {
                table[i][k] = rng.nextLong();
            }
        }
    }

    /** 全状态哈希（state[i]∈[0,kinds) 或 −1 空位；越域 fail-fast）。 */
    public long hash(long[] state) {
        long hash = 0L;
        for (int i = 0; i < state.length; i++) {
            if (state[i] == -1) {
                continue;
            }
            hash ^= codeAt(i, (int) state[i]);
        }
        return hash;
    }

    /** 增量放入（异或进——O(1)）。 */
    public long xorIn(long hash, int position, int kind) {
        return hash ^ codeAt(position, kind);
    }

    /** 增量移除（异或出——异或自逆）。 */
    public long xorOut(long hash, int position, int kind) {
        return hash ^ codeAt(position, kind);
    }

    /** 位置种类码读数（审计面；越域 fail-fast）。 */
    public long codeAt(int position, int kind) {
        if (position < 0 || position >= table.length
                || kind < 0 || kind >= table[position].length) {
            throw new IllegalArgumentException("越域: (" + position + "," + kind + ")");
        }
        return table[position][kind];
    }

    /** 位置数读数。 */
    public int positions() {
        return table.length;
    }

    /** 种类数读数。 */
    public int kinds() {
        return table[0].length;
    }
}
