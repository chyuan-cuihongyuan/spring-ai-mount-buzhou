package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Run 注册表内存实现（JDBC 实现见 store-jdbc 模块，契约一致）。 */
public class InMemoryRunRegistry implements RunRegistry {

    private final Map<String, RunStateSnapshot> runs = new ConcurrentHashMap<>();

    @Override
    public void save(RunStateSnapshot snapshot) {
        runs.put(snapshot.sessionId(), snapshot);
    }

    @Override
    public Optional<RunStateSnapshot> find(String sessionId) {
        return Optional.ofNullable(runs.get(sessionId));
    }

    @Override
    public List<RunStateSnapshot> list(RunStatus status) {
        return runs.values().stream()
                .filter(s -> s.status() == status)
                .sorted(Comparator.comparing(RunStateSnapshot::updatedAt))
                .toList();
    }

    /** impl-35 / spec 13 §stores-6：移除该会话的 run 快照（幂等）。 */
    @Override
    public void deleteSession(String sessionId) {
        runs.remove(sessionId);
    }

    /** impl-37 / spec 13 §growth-8：COMPLETED 保留窗口批删（在途/中断快照不受影响）。 */
    @Override
    public int pruneCompletedBefore(java.time.Instant cutoff) {
        return (int) runs.values().stream()
                .filter(s -> s.status() == RunStatus.COMPLETED && s.updatedAt().isBefore(cutoff))
                .map(s -> runs.remove(s.sessionId()))
                .filter(java.util.Objects::nonNull)
                .count();
    }
}
