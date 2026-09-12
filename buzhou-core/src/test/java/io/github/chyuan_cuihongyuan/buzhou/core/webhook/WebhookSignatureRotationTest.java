package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 540 / T833：双密钥轮换验签——current 命中、previous 命中（轮换窗
 * 内旧签名可验）、未知密钥 false、fail-closed、轮换×容差窗组合。
 */
class WebhookSignatureRotationTest {

    private static final String BODY = "{\"type\":\"session.closed\"}";

    @Test
    void previousSecretStillVerifiesDuringRotation() {
        String oldSig = WebhookSignatures.sign("old-secret", BODY);
        String newSig = WebhookSignatures.sign("new-secret", BODY);

        // 轮换期：新密钥验新签名、旧密钥验旧签名——都过
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", "old-secret", BODY, newSig)).isTrue();
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", "old-secret", BODY, oldSig)).isTrue();
        // 未知密钥签名不过
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", "old-secret", BODY,
                WebhookSignatures.sign("unknown", BODY))).isFalse();
    }

    @Test
    void rotationWithToleranceWindowCombines() {
        String sig = WebhookSignatures.sign("new-secret", BODY);
        long now = System.currentTimeMillis() / 1000;
        // 新鲜时间戳：过；过期时间戳：不过（fail-closed）
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", null, BODY, sig,
                String.valueOf(now), Duration.ofMinutes(5))).isTrue();
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", "old", BODY, sig,
                String.valueOf(now - 3600), Duration.ofMinutes(5))).isFalse();
        // 非数字时间戳 fail-closed
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", null, BODY, sig,
                "not-a-number", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void failClosedOnNullAndBlank() {
        String sig = WebhookSignatures.sign("new-secret", BODY);
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", null, BODY, sig)).isTrue();
        assertThat(WebhookSignatures.verifyWithRotation(null, null, BODY, sig)).isFalse();
        // blank previous = 无旧密钥——旧签名不过；current 有效签名照常过
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", "  ", BODY,
                WebhookSignatures.sign("old-secret", BODY))).isFalse();
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", null, null, sig)).isFalse();
        assertThat(WebhookSignatures.verifyWithRotation("new-secret", null, BODY, null)).isFalse();
    }
}
