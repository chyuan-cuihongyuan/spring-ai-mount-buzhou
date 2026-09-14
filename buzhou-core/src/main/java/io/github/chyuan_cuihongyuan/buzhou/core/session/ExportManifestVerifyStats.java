package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 导出清单校验统计读面（spec 1439 补位 / T2185 编号让位说明见台账：R46 =
 * effort #1439 补位 / 票复用 R39 重编号腾出的空缺号 T2179 + T2180
 * 见票文件）/ impl 1098。借鉴：TUF 仓库校验遥测（完整性校验的**执行频次与
 * 失败分布**本身是安全遥测——只建链不校验等于没建）。
 *
 * <p>进程级静态读面（ToolArgsValidator.validationStats 同款先例）：三个静态
 * verify 入口（canonical/subset/总入口）各点埋点——verifies/ok/mismatched/
 * missing/unexpected 五计数 + lastVerdictAt。守恒式：
 * {@code verifies = ok + mismatchedReports}（mismatched/missing/unexpected
 * 为明细桶非互斥可并存——一次校验可同时出现三类）。{@link #resetForTest()}
 * 归零注入点。
 */
public final class ExportManifestVerifyStats {

    private static final AtomicLong VERIFIES = new AtomicLong();
    private static final AtomicLong OK = new AtomicLong();
    private static final AtomicLong FAILED = new AtomicLong();
    private static final AtomicLong MISMATCHED_TOTAL = new AtomicLong();
    private static final AtomicLong MISSING_TOTAL = new AtomicLong();
    private static final AtomicLong UNEXPECTED_TOTAL = new AtomicLong();
    private static volatile String lastEntryKind = null;

    private ExportManifestVerifyStats() {
    }

    private static void record(ExportManifest.Verification v, String entryKind) {
        VERIFIES.incrementAndGet();
        lastEntryKind = entryKind;
        if (v == null || !v.ok()) {
            FAILED.incrementAndGet();
            MISMATCHED_TOTAL.addAndGet(v == null ? 0 : v.mismatchedIds().size());
            MISSING_TOTAL.addAndGet(v == null ? 0 : v.missingIds().size());
            UNEXPECTED_TOTAL.addAndGet(v == null ? 0 : v.unexpectedIds().size());
        } else {
            OK.incrementAndGet();
        }
    }


    /** 受踪包装：canonical 口径校验 + 统计入账。 */
    public static ExportManifest.Verification verifyCanonical(String manifestJson,
                                                              java.util.Map<String, String> contents) {
        var v = ExportManifest.verifyCanonical(manifestJson, contents);
        record(v, "canonical");
        return v;
    }

    /** 受踪包装：子集口径校验 + 统计入账。 */
    public static ExportManifest.Verification verifySubset(String manifestJson,
                                                           java.util.Map<String, String> contents) {
        var v = ExportManifest.verifySubset(manifestJson, contents);
        record(v, "subset");
        return v;
    }

    /** 受踪包装：全量口径校验 + 统计入账。 */
    public static ExportManifest.Verification verify(String manifestJson,
                                                     java.util.Map<String, String> contents) {
        var v = ExportManifest.verify(manifestJson, contents);
        record(v, "full");
        return v;
    }

    /** 只读快照：校验漏斗 + 三明细累计 + 末次入口。 */
    public static Snapshot stats() {
        return new Snapshot(VERIFIES.get(), OK.get(), FAILED.get(),
                MISMATCHED_TOTAL.get(), MISSING_TOTAL.get(), UNEXPECTED_TOTAL.get(),
                lastEntryKind);
    }

    /** 测试归零口。 */
    public static void resetForTest() {
        VERIFIES.set(0);
        OK.set(0);
        FAILED.set(0);
        MISMATCHED_TOTAL.set(0);
        MISSING_TOTAL.set(0);
        UNEXPECTED_TOTAL.set(0);
        lastEntryKind = null;
    }

    /**
     * @param verifies         累计校验次数
     * @param ok               全部一致次数
     * @param failed           存在异常的校验次数（verifies = ok + failed）
     * @param mismatchedTotal  摘要不匹配条目累计（明细桶）
     * @param missingTotal     清单缺失条目累计
     * @param unexpectedTotal  多余条目累计
     * @param lastEntryKind    末次校验入口（canonical/subset 总入口）
     */
    public record Snapshot(long verifies, long ok, long failed, long mismatchedTotal,
                           long missingTotal, long unexpectedTotal, String lastEntryKind) {

        /** 守恒式：verifies = ok + failed。 */
        public boolean conserved() {
            return verifies == ok + failed;
        }
    }
}
