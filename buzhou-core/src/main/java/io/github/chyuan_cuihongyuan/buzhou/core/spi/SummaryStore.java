package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;
import java.util.Optional;

public interface SummaryStore {

    long save(String sessionId, StructuredSummary summary);

    Optional<StructuredSummary> latest(String sessionId);

    List<StructuredSummary> history(String sessionId, int limit);

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的全部摘要版本（含版本计数器）。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }

    /**
     * impl-37 / spec 13 §stores-6：摘要旧版本修剪（etcd compaction 语义）——每会话只保留
     * 最近 {@code keepLatest} 个版本，返回删除的版本数。幂等；{@code keepLatest}＜1 按 1。
     * 默认 no-op（返回 0）。
     */
    default int pruneVersions(int keepLatest) {
        return 0;
    }
}
