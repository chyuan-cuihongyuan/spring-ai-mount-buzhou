package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

/**
 * 费斯妥网络（spec 8036 / V8073 / impl 2338）——
 * Feistel 1973 DES/Lucifer 思想：**64 位块分半、每轮
 * L↔R 交换 + R⊕F(K,R)——轮函数可逆性不要求，解密逆序
 * 同一轮函数**——全量 substitution 表巨大（轮函数黑盒
 * 即可逆）的病解。轮函数=SipHash24 密钥化派生低 32 位；
 * encrypt/decrypt 严格互逆；null fail-fast；确定性。
 *
 * <p>与 SipHash24（spec 8015）同族不同面：单向 PRF vs
 * 可逆结构。
 */
public final class FeistelNetwork {

    private static final int ROUNDS = 16;

    private FeistelNetwork() {
    }

    /** 加密 64 位块（null fail-fast 不适用——原语面 long）。 */
    public static long encrypt(long block, long key0, long key1) {
        long left = block >>> 32;
        long right = block & 0xFFFFFFFFL;
        for (int round = 0; round < ROUNDS; round++) {
            long next = left ^ roundFunction(right, key0, key1, round);
            left = right;
            right = next;
        }
        return (right << 32) | left;
    }

    /** 解密（逆序轮——与加密严格互逆）。 */
    public static long decrypt(long block, long key0, long key1) {
        long right = block >>> 32;
        long left = block & 0xFFFFFFFFL;
        for (int round = ROUNDS - 1; round >= 0; round--) {
            long previous = right ^ roundFunction(left, key0, key1, round);
            right = left;
            left = previous;
        }
        return (left << 32) | right;
    }

    /** 轮函数：SipHash24(轮序号派生密钥, 右半字节) 低 32 位。 */
    private static long roundFunction(long right, long key0, long key1, int round) {
        byte[] input = new byte[4];
        input[0] = (byte) right;
        input[1] = (byte) (right >>> 8);
        input[2] = (byte) (right >>> 16);
        input[3] = (byte) (right >>> 24);
        return SipHash24.hash(key0 + round, key1 ^ round, input) & 0xFFFFFFFFL;
    }
}
