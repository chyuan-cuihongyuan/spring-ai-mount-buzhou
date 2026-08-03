package io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentFingerprintTest {

    @Test
    void sameArgumentsProduceSameFingerprint() {
        Map<String, Object> args = Map.of("command", "deploy", "env", "prod");
        String fp1 = ArgumentFingerprint.fingerprint("run_command", args);
        String fp2 = ArgumentFingerprint.fingerprint("run_command", args);
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void fingerprintIs16HexChars() {
        String fp = ArgumentFingerprint.fingerprint("run_command", Map.of("command", "ls"));
        assertThat(fp).hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    void differentToolNameProducesDifferentFingerprint() {
        Map<String, Object> args = Map.of("command", "deploy");
        String fp1 = ArgumentFingerprint.fingerprint("run_command", args);
        String fp2 = ArgumentFingerprint.fingerprint("other_command", args);
        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    void differentArgumentsProduceDifferentFingerprint() {
        String fp1 = ArgumentFingerprint.fingerprint("run_command", Map.of("command", "deploy"));
        String fp2 = ArgumentFingerprint.fingerprint("run_command", Map.of("command", "rollback"));
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
        String fp1 = ArgumentFingerprint.fingerprint("tool", args1);
        String fp2 = ArgumentFingerprint.fingerprint("tool", args2);
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void emptyArgumentsProduceStableFingerprint() {
        String fp1 = ArgumentFingerprint.fingerprint("tool", Map.of());
        String fp2 = ArgumentFingerprint.fingerprint("tool", Map.of());
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void authKeyHasCorrectFormat() {
        String key = ArgumentFingerprint.authKey("run_command", "abc123def456abc7");
        assertThat(key).isEqualTo("auth.run_command.abc123def456abc7");
    }
}
