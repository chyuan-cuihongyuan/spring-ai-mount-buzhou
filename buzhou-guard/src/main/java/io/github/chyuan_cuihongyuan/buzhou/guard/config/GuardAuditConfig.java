package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * guard 审计配置（impl-39 / spec 13 §T64）：{@code buzhou.guard.audit.*} 子树解析。
 *
 * <pre>
 * buzhou.guard.audit.enabled=true                  # 默认随 guard 开
 * buzhou.guard.audit.store=auto                    # auto | jdbc | in-memory（auto=有 DataSource 即 jdbc）
 * buzhou.guard.audit.in-memory-capacity=4096
 * buzhou.guard.audit.signing.min-verify-version=0
 * buzhou.guard.audit.signing.keys[0].version=1
 * buzhou.guard.audit.signing.keys[0].private-key-path=/etc/buzhou/audit/v1.pem
 * buzhou.guard.audit.signing.keys[0].public-key-path=/etc/buzhou/audit/v1.pub  # 可选
 * buzhou.guard.audit.signing.key-dir=/etc/buzhou/audit                          # 目录扫描（spec 41 §A）：
 *                                                                               #   v<version>.pem 约定命名发现全部版本，
 *                                                                               #   运行期轮换写入的新钥重启后自动入环
 * </pre>
 *
 * @param enabled            审计能力开关（默认 true）
 * @param store              存储形态：auto / jdbc / in-memory
 * @param inMemoryCapacity   InMemory 环形容量（默认 4096）
 * @param minVerifyVersion   低于该版本的签名直接判不可验（0 = 不限）
 * @param keyFiles           版本化密钥文件集（最高版本 = active 签名钥；空 = 纯哈希链降级）
 * @param keyDir             spec 41 §A / T153：密钥目录扫描（v<version>.pem 约定命名；与
 *                           keyFiles 显式列表二选一，同时给出时合并；非空即启用轮换持久化）
 */
public record GuardAuditConfig(
        boolean enabled,
        String store,
        int inMemoryCapacity,
        int minVerifyVersion,
        List<KeyFile> keyFiles,
        Path keyDir) {

    public static final String STORE_AUTO = "auto";
    public static final String STORE_JDBC = "jdbc";
    public static final String STORE_IN_MEMORY = "in-memory";
    public static final int DEFAULT_CAPACITY = 4096;

    public record KeyFile(int version, Path privateKeyPath, Path publicKeyPath) {
    }

    public static GuardAuditConfig defaults() {
        return new GuardAuditConfig(true, STORE_AUTO, DEFAULT_CAPACITY, 0, List.of(), null);
    }

    /** {@code buzhou.guard} 子树中的 {@code audit} 子 Map 解析（缺失返回默认）。 */
    @SuppressWarnings("unchecked")
    public static GuardAuditConfig fromGuardMap(Map<String, Object> guardMap) {
        if (guardMap == null || !(guardMap.get("audit") instanceof Map<?, ?> raw)) {
            return defaults();
        }
        Map<String, Object> audit = (Map<String, Object>) raw;
        boolean enabled = !(audit.get("enabled") instanceof Boolean b) || b;
        String store = audit.get("store") instanceof String s && !s.isBlank()
                ? s.trim().toLowerCase() : STORE_AUTO;
        int capacity = audit.get("in-memory-capacity") instanceof Number n
                && n.intValue() > 0 ? n.intValue() : DEFAULT_CAPACITY;
        int minVerifyVersion = 0;
        List<KeyFile> keyFiles = new ArrayList<>();
        Path keyDir = null;
        if (audit.get("signing") instanceof Map<?, ?> signing) {
            Map<String, Object> signingMap = (Map<String, Object>) signing;
            minVerifyVersion = signingMap.get("min-verify-version") instanceof Number n
                    && n.intValue() >= 0 ? n.intValue() : 0;
            if (signingMap.get("key-dir") instanceof String dir && !dir.isBlank()) {
                keyDir = Path.of(dir.trim());
            }
            if (signingMap.get("keys") instanceof List<?> keys) {
                for (Object item : keys) {
                    if (item instanceof Map<?, ?> keyMap) {
                        KeyFile parsed = parseKeyFile((Map<String, Object>) keyMap);
                        if (parsed != null) {
                            keyFiles.add(parsed);
                        }
                    }
                }
            }
        }
        return new GuardAuditConfig(enabled, store, capacity, minVerifyVersion, List.copyOf(keyFiles),
                keyDir);
    }

    private static KeyFile parseKeyFile(Map<String, Object> keyMap) {
        Object versionVal = keyMap.get("version");
        Object privatePath = keyMap.get("private-key-path");
        if (!(versionVal instanceof Number version) || version.intValue() <= 0
                || !(privatePath instanceof String privateKey) || privateKey.isBlank()) {
            return null;
        }
        String publicKey = keyMap.get("public-key-path") instanceof String pub && !pub.isBlank()
                ? pub : null;
        return new KeyFile(version.intValue(), Path.of(privateKey),
                publicKey == null ? null : Path.of(publicKey));
    }
}
