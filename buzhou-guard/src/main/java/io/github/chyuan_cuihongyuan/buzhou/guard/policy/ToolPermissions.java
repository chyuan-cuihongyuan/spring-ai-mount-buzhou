package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色工具权限规则（spec 141 / T491，K8s RBAC / Casbin RBAC 借鉴）：角色 →
 * 工具名通配集（三形：精确名 / 前缀 {@code log*} / 全放 {@code *}）。
 *
 * <p><b>fail-closed</b>：未定义角色全拒（拼错角色名不是放行理由）；空集 = 该角色
 * 全拒。通配只做前缀/精确判定（无正则——防 ReDoS 面）。
 */
public final class ToolPermissions {

    /** 全放通配。 */
    public static final String ANY = "*";

    private final Map<String, List<String>> rules;
    /** impl-789 / spec 1037：判定分布四桶（守恒 checks == allowed + 两拒和）。 */
    private final java.util.concurrent.atomic.AtomicLong checks =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong allowedCount =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong deniedUndefinedRole =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong deniedByRules =
            new java.util.concurrent.atomic.AtomicLong();

    public ToolPermissions(Map<String, List<String>> roleToPatterns) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (roleToPatterns != null) {
            roleToPatterns.forEach((role, patterns) -> {
                if (role != null && !role.isBlank() && patterns != null) {
                    normalized.put(role, List.copyOf(patterns));
                }
            });
        }
        this.rules = Map.copyOf(normalized);
    }

    /** 已定义角色集（有界——tag 安全）。 */
    public Set<String> roles() {
        return rules.keySet();
    }

    /** 角色是否已定义。 */
    public boolean hasRole(String role) {
        return role != null && rules.containsKey(role);
    }

    /**
     * 裁决：未定义角色 = 拒（fail-closed）；已定义角色按通配集判。
     */
    public boolean allows(String role, String toolName) {
        checks.incrementAndGet(); // spec 1037：判定分布
        if (role == null || toolName == null || !rules.containsKey(role)) {
            deniedUndefinedRole.incrementAndGet();
            return false;
        }
        for (String pattern : rules.get(role)) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (pattern.equals(ANY) || pattern.equals(toolName)) {
                allowedCount.incrementAndGet();
                return true;
            }
            if (pattern.endsWith(ANY)
                    && toolName.startsWith(pattern.substring(0, pattern.length() - 1))) {
                allowedCount.incrementAndGet();
                return true;
            }
        }
        deniedByRules.incrementAndGet();
        return false;
    }

    /** 判定分布只读快照（守恒 checks == allowed + 两拒和——spec 1037）。 */
    public PermissionStats stats() {
        return new PermissionStats(checks.get(), allowedCount.get(),
                deniedUndefinedRole.get(), deniedByRules.get());
    }

    /** 判定分布计数行（不可变）。 */
    public record PermissionStats(long checks, long allowed, long deniedUndefinedRole,
                                  long deniedByRules) {
    }
}
