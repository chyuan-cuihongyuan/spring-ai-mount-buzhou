package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GuardAuditConfig.fromGuardMap 解析全分支补测（K 会话 R29 / spec 1228 / T1871——R7 逐类
 * 分支数据精定制导，~80 missed 集中区）：guard.audit 子 Map 解析的默认链、类型宽容、
 * key-dir/keys 合并、非法 KeyFile 过滤。先例：config 域纯函数解析测试（ConfigMaps 等）。
 */
class GuardAuditConfigFromGuardMapTest {

    private static Map<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void nullGuardMapFallsBackToDefaults() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(null);
        assertThat(c).isEqualTo(GuardAuditConfig.defaults());
        assertThat(c.enabled()).isTrue();
        assertThat(c.store()).isEqualTo("auto");
        assertThat(c.inMemoryCapacity()).isEqualTo(4096);
        assertThat(c.minVerifyVersion()).isZero();
        assertThat(c.keyFiles()).isEmpty();
        assertThat(c.keyDir()).isNull();
    }

    @Test
    void nonMapAuditValueFallsBackToDefaults() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(Map.of("audit", "不是Map"));
        assertThat(c).isEqualTo(GuardAuditConfig.defaults());
    }

    @Test
    void everyFieldRoundTrips() {
        var key = Map.of("version", 2, "private-key-path", "/k/v2.pem", "public-key-path", "/k/v2.pub");
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map(
                "audit", map("enabled", false, "store", "JDBC ", "in-memory-capacity", 77,
                        "signing", map("min-verify-version", 1, "key-dir", "/etc/audit",
                                "keys", List.of(key)))));

        assertThat(c.enabled()).isFalse();
        assertThat(c.store()).isEqualTo("jdbc"); // trim + lower
        assertThat(c.inMemoryCapacity()).isEqualTo(77);
        assertThat(c.minVerifyVersion()).isEqualTo(1);
        assertThat(c.keyDir()).isEqualTo(java.nio.file.Path.of("/etc/audit"));
        assertThat(c.keyFiles()).hasSize(1);
    }

    @Test
    void blankStoreAndInvalidCapacityFallBack() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map("audit", map(
                "store", "   ", "in-memory-capacity", 0)));

        assertThat(c.store()).isEqualTo("auto");
        assertThat(c.inMemoryCapacity()).isEqualTo(4096);
    }

    @Test
    void negativeMinVerifyVersionFallsBackToZero() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map("audit", map(
                "signing", map("min-verify-version", -3))));
        assertThat(c.minVerifyVersion()).isZero();
    }

    @Test
    void blankKeyDirIsIgnored() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map("audit", map(
                "signing", map("key-dir", "   "))));
        assertThat(c.keyDir()).isNull();
    }

    @Test
    void invalidKeyFilesAreFilteredOut() {
        // version 非法（0）/private-key-path 缺失/blank 的条目全部过滤，合法条目保留
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map("audit", map(
                "signing", map("keys", List.of(
                        map("version", 0, "private-key-path", "/k/a.pem"),
                        map("private-key-path", "/k/b.pem"),
                        map("version", 3, "private-key-path", "  "),
                        map("version", 5, "private-key-path", "/k/v5.pem"))))));

        assertThat(c.keyFiles()).hasSize(1);
        assertThat(c.keyFiles().get(0).version()).isEqualTo(5);
    }

    @Test
    void optionalPublicKeyPathDefaultsToNull() {
        GuardAuditConfig c = GuardAuditConfig.fromGuardMap(map("audit", map(
                "signing", map("keys", List.of(map("version", 4, "private-key-path", "/k/v4.pem"))))));

        assertThat(c.keyFiles().get(0).publicKeyPath()).isNull();
    }
}
