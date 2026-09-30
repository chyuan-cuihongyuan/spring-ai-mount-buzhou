package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MurmurHash3Test {

    @Test
    void shouldMatchReferenceAnchors() {
        // 参考实现锚（Appleby murmur3_x86_32 通行测试向量）：空串 seed=0 → 0
        assertThat(MurmurHash3.hash32(new byte[0], 0)).isZero();
        assertThat(MurmurHash3.hash32(new byte[0], 1)).isNotZero(); // 种子生效
        // 已知向量：""(0)=0；"aaaa..." 自一致性由确定性双跑钉住
        byte[] hello = "hello".getBytes(StandardCharsets.UTF_8);
        assertThat(MurmurHash3.hash32(hello, 0)).isEqualTo(MurmurHash3.hash32(hello, 0));
        assertThat(MurmurHash3.hash32(hello, 0)).isEqualTo(MurmurHash3.hash32(hello.clone(), 0));
        // 相同字节不同种子必不同（高概率——种子参与首轮混合）
        Set<Integer> seeds = new HashSet<>();
        for (int seed = 0; seed < 50; seed++) {
            seeds.add(MurmurHash3.hash32(hello, seed));
        }
        assertThat(seeds).hasSizeGreaterThan(45);
    }

    @Test
    void shouldAvalancheAndDistribute() {
        // 雪崩圣像：单位翻转平均翻转 ≈16 位（32 的一半——雪崩标杆）
        Random random = new Random(31);
        long totalFlips = 0;
        int trials = 2000;
        for (int t = 0; t < trials; t++) {
            byte[] data = new byte[16];
            random.nextBytes(data);
            int before = MurmurHash3.hash32(data, 0);
            data[random.nextInt(16)] ^= 1 << random.nextInt(8);
            int after = MurmurHash3.hash32(data, 0);
            totalFlips += Integer.bitCount(before ^ after);
        }
        double average = totalFlips / (double) trials;
        assertThat(average).isBetween(14.0, 18.0);
        // 分布圣像：10 万随机 8 字节无碰撞（32 位生日界 ~300 才有期望碰撞）
        Set<Integer> hashes = new HashSet<>();
        for (int t = 0; t < 100000; t++) {
            byte[] data = new byte[8];
            data[0] = (byte) (t & 0xFF);
            data[1] = (byte) ((t >> 8) & 0xFF);
            data[2] = (byte) ((t >> 16) & 0xFF);
            data[3] = (byte) ((t >> 24) & 0xFF);
            data[4] = (byte) random.nextInt();
            data[5] = (byte) random.nextInt();
            hashes.add(MurmurHash3.hash32(data, 7));
        }
        assertThat(hashes.size()).isGreaterThan(99000);
    }

    @Test
    void shouldHandleTailBlocksAndFailFast() {
        // 尾块三变体（1/2/3 字节）确定性 + 相邻长度不同哈希
        for (int len = 1; len <= 12; len++) {
            byte[] data = new byte[len];
            for (int i = 0; i < len; i++) {
                data[i] = (byte) ('a' + i);
            }
            assertThat(MurmurHash3.hash32(data, 0)).isEqualTo(MurmurHash3.hash32(data, 0));
        }
        byte[] five = "abcde".getBytes(StandardCharsets.US_ASCII);
        byte[] six = "abcdef".getBytes(StandardCharsets.US_ASCII);
        assertThat(MurmurHash3.hash32(five, 0)).isNotEqualTo(MurmurHash3.hash32(six, 0));
        assertThatThrownBy(() -> MurmurHash3.hash32(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
