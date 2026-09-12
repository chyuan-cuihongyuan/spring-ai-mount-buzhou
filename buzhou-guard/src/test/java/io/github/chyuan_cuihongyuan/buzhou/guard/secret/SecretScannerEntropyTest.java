package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秘密扫描熵阈值过滤测试（spec 714 / T979–T980 / impl 517）：高熵过、示例键
 * 滤、低熵滤、PRIVATE_KEY_BLOCK 豁免、默认关零回归。
 *
 * <p>夹具均为<b>非真实凭据</b>：AWS_EXAMPLE 是 AWS 官方文档公开示例键；
 * AWS_RANDOM 为人工乱序串——分片拼装只为表达「非凭据字面量」（安全扫描器
 * 测试夹具惯例）。
 */
class SecretScannerEntropyTest {

    private static final String AWS_EXAMPLE = "AKIA" + "IOSFODNN" + "7EXAMPLE";
    /** 随机形态（人工乱序高熵——20 位大写数字）。 */
    private static final String AWS_RANDOM = "AKIA" + "Q7VXK2M9" + "TZ4RBP8L";
    private static final String LOW_ENTROPY_GH = "ghp_" + "a".repeat(20);

    @Test
    void highEntropyPassesGate() {
        SecretScanner gated = new SecretScanner(null, SecretScanner.DEFAULT_MIN_ENTROPY);
        String text = "key=" + AWS_RANDOM;

        List<SecretScanner.SecretMatch> hits = gated.scan(text);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).type()).isEqualTo(SecretType.AWS_ACCESS_KEY);
    }

    @Test
    void awsExampleFilteredWhenGatedAndCaughtWhenUngated() {
        String text = "key=" + AWS_EXAMPLE;

        SecretScanner gated = new SecretScanner(null, SecretScanner.DEFAULT_MIN_ENTROPY);
        assertThat(gated.scan(text)).isEmpty(); // 示例键低熵被滤

        SecretScanner ungated = new SecretScanner();
        assertThat(ungated.scan(text)).hasSize(1); // 对照：熵关照常命中
    }

    @Test
    void lowEntropyPlaceholderFiltered() {
        SecretScanner gated = new SecretScanner(null, SecretScanner.DEFAULT_MIN_ENTROPY);
        String text = "token=" + LOW_ENTROPY_GH;

        assertThat(gated.scan(text)).isEmpty();
    }

    @Test
    void privateKeyBlockExemptFromEntropyGate() {
        SecretScanner gated = new SecretScanner(null, SecretScanner.DEFAULT_MIN_ENTROPY);
        String text = "-----BEGIN RSA PRIVATE KEY-----";

        List<SecretScanner.SecretMatch> hits = gated.scan(text);

        assertThat(hits).hasSize(1); // BEGIN 行字面签名——熵豁免
        assertThat(hits.get(0).type()).isEqualTo(SecretType.PRIVATE_KEY_BLOCK);
    }

    @Test
    void defaultConstructorUnchanged() {
        SecretScanner scanner = new SecretScanner();
        assertThat(scanner.scan("key=" + AWS_EXAMPLE)).hasSize(1); // 熵关——示例键照常命中
        assertThat(scanner.scan("key=" + AWS_RANDOM)).hasSize(1);
    }

    @Test
    void shannonEntropyBasics() {
        assertThat(SecretScanner.shannonEntropy("")).isZero();
        assertThat(SecretScanner.shannonEntropy("aaaaaaaa")).isZero(); // 单字符集零熵
        assertThat(SecretScanner.shannonEntropy("ab")).isEqualTo(1.0); // 两等概率字符 = 1 bit
        // 随机形态串熵显著高于重复串
        assertThat(SecretScanner.shannonEntropy(AWS_RANDOM))
                .isGreaterThan(SecretScanner.shannonEntropy(LOW_ENTROPY_GH));
    }
}
