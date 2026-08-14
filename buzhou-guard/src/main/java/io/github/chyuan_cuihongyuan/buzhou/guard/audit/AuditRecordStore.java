package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.List;

/**
 * 审计记录持久化 SPI（impl-39 / spec 13 §T64）：append-only（只增不改不删），
 * 跨重启可全量重放（{@link #loadAll()} 供 {@link AuditChainVerifier} 独立校验与链续接）。
 *
 * <p>内置实现：{@link InMemoryAuditRecordStore}（有界环形，轻量部署/测试）、
 * {@link JdbcAuditRecordStore}（JDBC append-only 表）。append 失败明示抛出
 * （由装配层决定降级策略，SPI 本身不静默吞审计）。
 */
public interface AuditRecordStore {

    /** 追加一条审计记录（append-only；实现须线程安全）。 */
    void append(AgentAuditRecord record);

    /** 按追加序全量加载（校验/续链输入；InMemory 实现只含环形窗口内的近段记录）。 */
    List<AgentAuditRecord> loadAll();

    /** 当前可加载记录数。 */
    long count();
}
