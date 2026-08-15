package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Store 一致性校验工具 fsck（spec 29 / T108 / impl-83）：跨 store 对账——
 * 以观测记录的会话全集为基准，检测孤儿摘要 / 残留状态 / 泄漏租约 / 悬挂观测。
 *
 * <p><b>会话全集口径（诚实声明）</b>：观测 store 的 {@code listSessionSummaries} 分页
 * （数字游标约定）+ 调用方补充 {@code extraSessionIds}——全集完整性依赖观测记录；
 * 观测未留痕的会话不在校验面（可经 extras 补充）。合成会话
 * {@code __buzhou.webhook__}（spec 24 outbox）不在全集内，天然豁免。
 *
 * <p><b>修复策略</b>：{@link #run} 只读报告；{@link #repair} 按检测项选择清除
 * （默认全 false 不动）；悬挂观测<b>永不自动清</b>（审计保留价值）。
 * spill 侧孤儿由 spill 自有机制治理（spec 26 引用账本 + sweepOrphans），不在本工具面。
 */
public final class StoreFsck {

    private static final int PAGE = 100;
    private static final int MAX_PAGES = 10_000;

    private StoreFsck() {
    }

    /** 只读校验（会话全集 = 观测记录）。 */
    public static StoreIntegrityReport run(BuzhouStores stores) {
        return run(stores, Set.of());
    }

    /** 只读校验（观测全集 + 调用方补充的 sessionIds）。 */
    public static StoreIntegrityReport run(BuzhouStores stores, Set<String> extraSessionIds) {
        Set<String> universe = sessionUniverse(stores, extraSessionIds);
        List<StoreIntegrityReport.Finding> findings = new ArrayList<>();
        for (String sessionId : universe) {
            boolean hasMessages = !stores.messageStore().load(sessionId).isEmpty();
            boolean hasSummary = stores.summaryStore().latest(sessionId).isPresent();
            boolean hasState = !stores.sessionStateStore().getAll(sessionId).isEmpty();
            if (!hasMessages && hasSummary) {
                findings.add(new StoreIntegrityReport.Finding(sessionId,
                        StoreIntegrityReport.ORPHAN_SUMMARY, StoreIntegrityReport.Severity.WARN,
                        "摘要存在但消息为空"));
            }
            if (!hasMessages && !hasSummary && hasState) {
                findings.add(new StoreIntegrityReport.Finding(sessionId,
                        StoreIntegrityReport.STATE_RESIDUE, StoreIntegrityReport.Severity.INFO,
                        "三槽皆空但残留 " + stores.sessionStateStore().getAll(sessionId).size() + " 个 state 键"));
            }
            if (!hasMessages && !hasSummary && !hasState) {
                // 会话在全集（观测留痕）但数据全空：可能是观测先于首聊的中断会话
                findings.add(new StoreIntegrityReport.Finding(sessionId,
                        StoreIntegrityReport.DANGLING_OBSERVABILITY, StoreIntegrityReport.Severity.INFO,
                        "观测留痕但会话数据全空（只报不清）"));
            }
            if (!hasMessages && stores.sessionLeaseStore().inspect(sessionId).isPresent()) {
                findings.add(new StoreIntegrityReport.Finding(sessionId,
                        StoreIntegrityReport.DANGLING_LEASE, StoreIntegrityReport.Severity.WARN,
                        "租约存在但会话无消息（疑似泄漏，占用容量语义）"));
            }
        }
        return new StoreIntegrityReport(findings);
    }

    /** 修复选项（默认全 false——只报不清是 safe-by-default；观测记录永不自动清）。 */
    public record RepairOptions(boolean removeOrphanSummaries, boolean clearStateResidue,
            boolean releaseDanglingLeases) {
        public static RepairOptions none() {
            return new RepairOptions(false, false, false);
        }
    }

    /** 按检测项清除；返回各检测项实际清除的会话数（按修复执行顺序：摘要→state→租约）。 */
    public static java.util.Map<String, Integer> repair(BuzhouStores stores,
            StoreIntegrityReport report, RepairOptions options) {
        java.util.Map<String, Integer> repaired = new java.util.LinkedHashMap<>();
        if (options.removeOrphanSummaries()) {
            int n = 0;
            for (StoreIntegrityReport.Finding f : report.findings()) {
                if (f.check().equals(StoreIntegrityReport.ORPHAN_SUMMARY)) {
                    stores.summaryStore().deleteSession(f.sessionId());
                    n++;
                }
            }
            repaired.put(StoreIntegrityReport.ORPHAN_SUMMARY, n);
        }
        if (options.clearStateResidue()) {
            int n = 0;
            for (StoreIntegrityReport.Finding f : report.findings()) {
                if (f.check().equals(StoreIntegrityReport.STATE_RESIDUE)) {
                    stores.sessionStateStore().deleteSession(f.sessionId());
                    n++;
                }
            }
            repaired.put(StoreIntegrityReport.STATE_RESIDUE, n);
        }
        if (options.releaseDanglingLeases()) {
            int n = 0;
            for (StoreIntegrityReport.Finding f : report.findings()) {
                if (f.check().equals(StoreIntegrityReport.DANGLING_LEASE)) {
                    stores.sessionLeaseStore().deleteSession(f.sessionId());
                    n++;
                }
            }
            repaired.put(StoreIntegrityReport.DANGLING_LEASE, n);
        }
        return repaired;
    }

    /** 观测 store 数字游标分页（内存/JDBC/Redis 实现共同约定：offset 字符串）。 */
    private static Set<String> sessionUniverse(BuzhouStores stores, Set<String> extraSessionIds) {
        Set<String> universe = new LinkedHashSet<>();
        int offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary> summaries =
                    stores.observabilityStore().listSessionSummaries(String.valueOf(offset), PAGE);
            summaries.forEach(s -> universe.add(s.sessionId()));
            if (summaries.size() < PAGE) {
                break;
            }
            offset += PAGE;
        }
        if (extraSessionIds != null) {
            universe.addAll(extraSessionIds);
        }
        return universe;
    }
}
