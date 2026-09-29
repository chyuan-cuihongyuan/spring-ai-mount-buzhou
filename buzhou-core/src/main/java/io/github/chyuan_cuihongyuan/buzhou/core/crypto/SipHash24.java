package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

/**
 * SipHash-2-4（spec 8015 / V8031 / impl 2317）——
 * Aumasson & Bernstein 2012 思想（Redis/Python/Rust HashMap
 * 哈希 DoS 防御同源）：**64 位密钥化的 PRF 哈希**——每 8 字
 * 节块 2 轮压缩、终化 4 轮（名字即参数）——无钥哈希可被对抗
 * 者离线预造全碰撞键集（哈希 flood 让表退化成链）的病解。
 * 官方向量钉死实现正确性；同键同数据确定性、异密钥异值；
 * 非加密承诺明示（面向哈希表防护，不做 MAC 级承诺）；
 * null 数据 fail-fast；纯函数（状态为调用栈局部——勘误：
 * 初版 round 用静态字段且与局部态脱节——State 载体钉住修正）。
 *
 * <p>与 XxHash64（spec 7030）同族不同面：非加密高速哈希 vs
 * 密钥化对抗碰撞预计算。
 */
public final class SipHash24 {

    private static final long V0_SEED = 0x736f6d6570736575L;
    private static final long V1_SEED = 0x646f72616e646f6dL;
    private static final long V2_SEED = 0x6c7967656e657261L;
    private static final long V3_SEED = 0x7465646279746573L;

    private SipHash24() {
    }

    /** SipHash-2-4（null 数据 fail-fast）。 */
    public static long hash(long k0, long k1, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("数据非空引用");
        }
        State s = new State(
                V0_SEED ^ k0,
                V1_SEED ^ k1,
                V2_SEED ^ k0,
                V3_SEED ^ k1);
        int i = 0;
        int limit = data.length - (data.length % 8);
        while (i < limit) {
            long m = loadLittleEndian(data, i);
            s.v3 ^= m;
            s.round();
            s.round();
            s.v0 ^= m;
            i += 8;
        }
        long last = ((long) (data.length & 0xff)) << 56;
        int shift = 0;
        while (i < data.length) {
            last |= (data[i] & 0xffL) << (shift * 8);
            shift++;
            i++;
        }
        s.v3 ^= last;
        s.round();
        s.round();
        s.v0 ^= last;
        s.v2 ^= 0xffL;
        s.round();
        s.round();
        s.round();
        s.round();
        return s.v0 ^ s.v1 ^ s.v2 ^ s.v3;
    }

    /** SipRound 状态载体（调用栈局部——纯函数语义）。 */
    private static final class State {
        private long v0;
        private long v1;
        private long v2;
        private long v3;

        private State(long v0, long v1, long v2, long v3) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }

        /** SipRound：两次 ARX 半轮（13/16/21/17 旋转 + 32 位位翻转）。 */
        private void round() {
            v0 += v1;
            v1 = Long.rotateLeft(v1, 13) ^ v0;
            v0 = Long.rotateLeft(v0, 32);
            v2 += v3;
            v3 = Long.rotateLeft(v3, 16) ^ v2;
            v0 += v3;
            v3 = Long.rotateLeft(v3, 21) ^ v0;
            v2 += v1;
            v1 = Long.rotateLeft(v1, 17) ^ v2;
            v2 = Long.rotateLeft(v2, 32);
        }
    }

    private static long loadLittleEndian(byte[] data, int offset) {
        return (data[offset] & 0xffL)
                | (data[offset + 1] & 0xffL) << 8
                | (data[offset + 2] & 0xffL) << 16
                | (data[offset + 3] & 0xffL) << 24
                | (data[offset + 4] & 0xffL) << 32
                | (data[offset + 5] & 0xffL) << 40
                | (data[offset + 6] & 0xffL) << 48
                | (data[offset + 7] & 0xffL) << 56;
    }
}
