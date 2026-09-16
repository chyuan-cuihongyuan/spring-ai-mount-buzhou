package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 必选检查聚合（spec 2031 / T3163 / impl 1582）——GitHub required
 * status checks rollup 思想：多检查 → 单结论的门禁语义——任一必选
 * FAILURE → FAILURE（一票否决）；否则任一必选 PENDING（含未报）→
 * PENDING（门未关）；全部必选 SUCCESS → SUCCESS（放行）。可选检查
 * 不阻断但显形（观测面——可选退化早期预警）。
 *
 * <p>synchronized 小临界区；注册序稳定（快照可读）。
 */
public final class RequiredChecksRollup {

    /** 单检查三态。 */
    public enum CheckState {
        SUCCESS,
        FAILURE,
        PENDING
    }

    /** 聚合结论三态。 */
    public enum Rollup {
        SUCCESS,
        FAILURE,
        PENDING
    }

    private record Registration(boolean required, CheckState state, boolean reported) {
    }

    private final Map<String, Registration> checks = new LinkedHashMap<>();

    /** 注册检查（required=true 阻断门；false 可选——仅显形）。契约：name 非空非重复。 */
    public synchronized void register(String name, boolean required) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        if (checks.containsKey(name)) {
            throw new IllegalArgumentException("检查已注册：" + name);
        }
        checks.put(name, new Registration(required, CheckState.PENDING, false));
    }

    /** 上报检查状态（未注册 fail-fast）。 */
    public synchronized void report(String name, CheckState state) {
        Registration r = checks.get(name);
        if (r == null) {
            throw new IllegalArgumentException("检查未注册：" + name);
        }
        if (state == null) {
            throw new IllegalArgumentException("state 不能为 null");
        }
        checks.put(name, new Registration(r.required(), state, true));
    }

    /** 聚合：任一必选 FAILURE → FAILURE；任一必选 PENDING/未报 → PENDING；全必选 SUCCESS → SUCCESS。 */
    public synchronized Rollup rollup() {
        boolean anyPending = false;
        for (Registration r : checks.values()) {
            if (!r.required()) {
                continue; // 可选不阻断
            }
            if (r.state() == CheckState.FAILURE) {
                return Rollup.FAILURE; // 一票否决（优先于 pending——已失败不必等）
            }
            if (r.state() == CheckState.PENDING) {
                anyPending = true;
            }
        }
        return anyPending ? Rollup.PENDING : Rollup.SUCCESS;
    }

    /** 各检查快照（注册序：name → required/state/reported）。 */
    public synchronized Map<String, CheckState> states() {
        Map<String, CheckState> snapshot = new LinkedHashMap<>();
        checks.forEach((name, r) -> snapshot.put(name, r.state()));
        return snapshot;
    }

    /** 账面：总检查/必选数/未报数/可选失败数（可选退化显形）。 */
    public synchronized RollupStats stats() {
        int required = 0;
        int unreported = 0;
        int optionalFailures = 0;
        for (Registration r : checks.values()) {
            if (r.required()) {
                required++;
                if (!r.reported()) {
                    unreported++;
                }
            } else if (r.state() == CheckState.FAILURE) {
                optionalFailures++;
            }
        }
        return new RollupStats(checks.size(), required, unreported, optionalFailures);
    }

    /** 聚合账快照。 */
    public record RollupStats(int totalChecks, int requiredChecks,
                              int unreportedRequired, int optionalFailures) {
    }
}
