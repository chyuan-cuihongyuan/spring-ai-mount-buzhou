package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空闲连接收割（spec 1924 / T3049 / impl 1525）——HikariCP idle
 * reaper 语义：连接空闲 ≥ maxIdle 即入选收割（边界含上），闲置
 * 最久的排前（有配额时先收最旧的）；活跃连接永不入选。
 *
 * <p>纯函数零状态；真实关闭归连接池。
 */
public final class IdleConnectionReaper {

    private IdleConnectionReaper() {
    }

    /**
     * 收割候选：闲置 ≥ maxIdle 的连接键，按闲置时长降序（最旧优先）。
     * 契约：lastUsedByConn 非空、now ≥ 每个 lastUsed、maxIdle ≥ 1
     * （fail-fast）。
     */
    public static java.util.List<String> reapCandidates(
            Map<String, Long> lastUsedByConn, long nowMillis, long maxIdleMillis) {
        if (lastUsedByConn == null || lastUsedByConn.isEmpty()) {
            throw new IllegalArgumentException("lastUsedByConn 不能为空");
        }
        if (nowMillis < 0) {
            throw new IllegalArgumentException("now 不能为负：" + nowMillis);
        }
        if (maxIdleMillis < 1) {
            throw new IllegalArgumentException(
                    "maxIdleMillis 不能小于 1：" + maxIdleMillis);
        }
        return lastUsedByConn.entrySet().stream()
                .peek(e -> {
                    if (e.getValue() == null || e.getValue() < 0 || e.getValue() > nowMillis) {
                        throw new IllegalArgumentException(
                                "lastUsed 非法（为负/为空/晚于 now）：" + e.getKey());
                    }
                })
                .filter(e -> nowMillis - e.getValue() >= maxIdleMillis)
                .sorted(Comparator.comparingLong(
                        (Map.Entry<String, Long> e) -> e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 闲置时长读数：now − lastUsed。契约：now ≥ lastUsed ≥ 0
     * （fail-fast）。
     */
    public static long idleMillis(long lastUsedMillis, long nowMillis) {
        if (lastUsedMillis < 0) {
            throw new IllegalArgumentException(
                    "lastUsed 不能为负：" + lastUsedMillis);
        }
        if (nowMillis < lastUsedMillis) {
            throw new IllegalArgumentException(String.format(
                    "now 早于 lastUsed：%d < %d", nowMillis, lastUsedMillis));
        }
        return nowMillis - lastUsedMillis;
    }
}
