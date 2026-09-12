package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.function.IntSupplier;

/**
 * 会话最小可用水位闸（spec 704 / T959，k8s PodDisruptionBudget 借鉴）：归档
 * （自愿驱逐唯一入口）执行前检查存活会话数——低于 minAvailable 拒绝摘除，
 * 故障/高峰期运维动作不再进一步削薄在线容量。
 *
 * <p><b>fail-open 语义</b>：{@code liveSessions} 供应商返回负数（计数源缺席/未知）
 * 时<b>放行</b>——保底闸失明不误伤运维动作（健康 UNKNOWN 诚实语义同款）。
 * restore/purge 不受闸（还原恢复容量；purge 是冷层 TTL 治理）。
 */
public final class SessionAvailabilityFloor {

    private final int minAvailable;
    private final IntSupplier liveSessions;

    public SessionAvailabilityFloor(int minAvailable, IntSupplier liveSessions) {
        if (minAvailable < 0) {
            throw new IllegalArgumentException(
                    "minAvailable 必须 >=0（当前 " + minAvailable + "）");
        }
        if (liveSessions == null) {
            throw new IllegalArgumentException("liveSessions 供应商必须非空");
        }
        this.minAvailable = minAvailable;
        this.liveSessions = liveSessions;
    }

    /** minAvailable（观测/编程面）。 */
    public int minAvailable() {
        return minAvailable;
    }

    /** 归档是否放行：存活数 > minAvailable；计数未知（负数）fail-open 放行。 */
    public boolean allowsArchive() {
        int live = liveSessions.getAsInt();
        return live < 0 || live > minAvailable;
    }
}
