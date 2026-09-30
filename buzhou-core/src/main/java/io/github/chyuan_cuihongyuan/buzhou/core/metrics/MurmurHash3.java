package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * MurmurHash3 x86 32 位哈希（spec 9036 / W9073 / impl 2389）——
 * Appleby 2008 MurmurHash3 思想（Guava hashing/Redis dict/
 * Cassandra partitioner 同源）：**乘法散列 + 32 位旋转混合的
 * 三轮终结化（fmix32）——非加密高速哈希的雪崩标杆**——
 * String.hashCode 单乘弱雪崩（相邻输入高位不变）的病解。
 * 种子入参（确定性纯函数——同种子同输出完全可回放）；
 * 尾块三变体处理（≤3 字节显式合并）；32 位结果契约；
 * null fail-fast；空输入返回种子终结值（定义良置）。
 *
 * <p>与 SipHash24（crypto 域）同域不同面：抗 DoS 密钥化慢哈希
 * vs 高速非密钥哈希；与 ZobristHashing（同包）不同面：异或表
 * 增量 vs 一次成型散列；与 DeterministicHash（同包）互补：
 * 通用面归它、雪崩强度归此。
 */
public final class MurmurHash3 {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private MurmurHash3() {
    }

    /**
     * 32 位哈希（seed 种子——同输入同种子必同输出）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static int hash32(byte[] data, int seed) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int length = data.length;
        int blocks = length / 4;
        int hash = seed;
        for (int i = 0; i < blocks; i++) {
            int k = ((data[i * 4] & 0xFF))
                    | ((data[i * 4 + 1] & 0xFF) << 8)
                    | ((data[i * 4 + 2] & 0xFF) << 16)
                    | ((data[i * 4 + 3] & 0xFF) << 24);
            k *= C1;
            k = Integer.rotateLeft(k, 15);
            k *= C2;
            hash ^= k;
            hash = Integer.rotateLeft(hash, 13);
            hash = hash * 5 + 0xe6546b64;
        }
        int tail = blocks * 4;
        int k1 = 0;
        switch (length & 3) {
            case 3:
                k1 ^= (data[tail + 2] & 0xFF) << 16;
                // fall through
            case 2:
                k1 ^= (data[tail + 1] & 0xFF) << 8;
                // fall through
            case 1:
                k1 ^= data[tail] & 0xFF;
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= C2;
                hash ^= k1;
                // fall through
            default:
                break;
        }
        hash ^= length;
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        return hash;
    }
}
