package io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentFingerprintTest {

    @Test
    void sameArgumentsProduceSameFingerprint() {
        Map<String, Object> args = Map.of("command", "deploy", "env", "prod");
        String fp1 = ArgumentFingerprint.fingerprint(args);
        String fp2 = ArgumentFingerprint.fingerprint(args);
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void fingerprintIs16HexChars() {
        String fp = ArgumentFingerprint.fingerprint(Map.of("command", "ls"));
        assertThat(fp).hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    void fingerprintFollowsSpecFormula() {
        // spec 07：fingerprint = SHA-256(canonicalJson(arguments)) 前 16 hex，工具名不入哈希材质
        String fp = ArgumentFingerprint.fingerprint(Map.of("command", "deploy"));
        assertThat(fp).isEqualTo(sha256Hex16("{\"command\":\"deploy\"}"));
    }

    @Test
    void differentArgumentsProduceDifferentFingerprint() {
        String fp1 = ArgumentFingerprint.fingerprint(Map.of("command", "deploy"));
        String fp2 = ArgumentFingerprint.fingerprint(Map.of("command", "rollback"));
        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    void keyOrderDoesNotAffectFingerprint() {
        // LinkedHashMap 保证遍历序 = 插入序；故意用相反插入序
        Map<String, Object> args1 = new LinkedHashMap<>();
        args1.put("a", "1");
        args1.put("b", "2");
        Map<String, Object> args2 = new LinkedHashMap<>();
        args2.put("b", "2");
        args2.put("a", "1");
        String fp1 = ArgumentFingerprint.fingerprint(args1);
        String fp2 = ArgumentFingerprint.fingerprint(args2);
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void nestedKeyOrderInsideListDoesNotAffectFingerprint() {
        Map<String, Object> args1 = Map.of("items", List.of(
                new LinkedHashMap<>(Map.of("x", "1", "y", "2"))));
        Map<String, Object> args2 = Map.of("items", List.of(
                new LinkedHashMap<>(Map.of("y", "2", "x", "1"))));
        assertThat(ArgumentFingerprint.fingerprint(args1))
                .isEqualTo(ArgumentFingerprint.fingerprint(args2));
    }

    @Test
    void emptyArgumentsProduceStableFingerprint() {
        String fp1 = ArgumentFingerprint.fingerprint(Map.of());
        String fp2 = ArgumentFingerprint.fingerprint(Map.of());
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void authKeyHasCorrectFormat() {
        String key = ArgumentFingerprint.authKey("run_command", "abc123def456abc7");
        assertThat(key).isEqualTo("auth.run_command.abc123def456abc7");
    }

    private static String sha256Hex16(String material) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
