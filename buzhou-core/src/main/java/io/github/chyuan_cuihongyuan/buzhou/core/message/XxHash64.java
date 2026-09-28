package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * xxHash64（spec 7030 / U7261 / impl 2282）——Collet xxHash
 * 思想（Kafka/ZSTD 同源）：**四累加器 32 字节条带旋转混合
 * + 雪崩终洗**——质量与速度兼顾的非加密哈希（JDK 内建
 * 哈希质量不足、SHA 过重两种病的折中解）。官方钉子向量
 * （空串/单字符/abc——seed 0）+stripe 路径与尾字节路径
 * 一致性。确定性纯函数（无密钥无随机）。
 *
 * <p>与 Crc32C（同包）同族不同面：线性校验码（防传输
 * 失真）vs 雪崩哈希（防构造碰撞——均非加密承诺，明示）。
 */
public final class XxHash64 {

    private static final long PRIME1 = -7046029288634856825L; // 0x9e3779b185ebca87
    private static final long PRIME2 = -4417276706812531889L; // 0xc2b2ae3d27d4eb4f
    private static final long PRIME3 = 1609587929392839161L; // 0x165667b19e3779f9
    private static final long PRIME4 = -8796714831421723037L; // 0x85ebca77c2b2ae63
    private static final long PRIME5 = 2870177450012600261L; // 0x27d4eb2f165667c5

    private XxHash64() {
    }

    /** 64 位哈希（seed=0 便捷面；null fail-fast）。 */
    public static long hash(byte[] data) {
        return hash(data, 0L);
    }

    /** 64 位哈希（带种子；null fail-fast）。 */
    public static long hash(byte[] data, long seed) {
        if (data == null) {
            throw new IllegalArgumentException("数据非空");
        }
        int length = data.length;
        int offset = 0;
        long hash;
        if (length >= 32) {
            long v1 = seed + PRIME1 + PRIME2;
            long v2 = seed + PRIME2;
            long v3 = seed;
            long v4 = seed - PRIME1;
            int limit = length - 32;
            do {
                v1 = mixAccumulator(v1, readLong(data, offset));
                v2 = mixAccumulator(v2, readLong(data, offset + 8));
                v3 = mixAccumulator(v3, readLong(data, offset + 16));
                v4 = mixAccumulator(v4, readLong(data, offset + 24));
                offset += 32;
            } while (offset <= limit);
            hash = Long.rotateLeft(v1, 1) + Long.rotateLeft(v2, 7)
                    + Long.rotateLeft(v3, 12) + Long.rotateLeft(v4, 18);
            hash = mergeRound(hash, v1);
            hash = mergeRound(hash, v2);
            hash = mergeRound(hash, v3);
            hash = mergeRound(hash, v4);
        } else {
            hash = seed + PRIME5;
        }
        hash += length;
        while (offset + 8 <= length) {
            long lane = readLong(data, offset);
            hash ^= mix(0, lane);
            hash = Long.rotateLeft(hash, 27) * PRIME1 + PRIME4;
            offset += 8;
        }
        if (offset + 4 <= length) {
            hash ^= (readInt(data, offset) & 0xFFFFFFFFL) * PRIME1;
            hash = Long.rotateLeft(hash, 23) * PRIME2 + PRIME3;
            offset += 4;
        }
        while (offset < length) {
            hash ^= (data[offset] & 0xFFL) * PRIME5;
            hash = Long.rotateLeft(hash, 11) * PRIME1;
            offset++;
        }
        hash ^= hash >>> 33;
        hash *= PRIME2;
        hash ^= hash >>> 29;
        hash *= PRIME3;
        hash ^= hash >>> 32;
        return hash;
    }

    private static long mixAccumulator(long current, long lane) {
        return Long.rotateLeft(current + lane * PRIME2, 31) * PRIME1;
    }

    private static long mergeRound(long hash, long value) {
        value = mix(0, value);
        hash ^= value;
        return hash * PRIME1 + PRIME4;
    }

    private static long mix(long ignore, long lane) {
        return Long.rotateLeft(lane * PRIME2, 31) * PRIME1;
    }

    private static long readLong(byte[] data, int offset) {
        return (data[offset] & 0xFFL)
                | (data[offset + 1] & 0xFFL) << 8
                | (data[offset + 2] & 0xFFL) << 16
                | (data[offset + 3] & 0xFFL) << 24
                | (data[offset + 4] & 0xFFL) << 32
                | (data[offset + 5] & 0xFFL) << 40
                | (data[offset + 6] & 0xFFL) << 48
                | (data[offset + 7] & 0xFFL) << 56;
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | (data[offset + 1] & 0xFF) << 8
                | (data[offset + 2] & 0xFF) << 16
                | (data[offset + 3] & 0xFF) << 24;
    }
}
