package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话导出条件协商（spec 710 / T971，HTTP ETag / If-None-Match / RFC 7232 借鉴）：
 * 周期同步方（备份/下游流水线/多实例巡检）携上轮内容指纹发起——
 * {@link #exportIfChanged} 指纹相等返回 {@link Status#UNCHANGED}（payload 不外发，
 * 同步方跳过写入）；否则 {@link Status#EXPORTED} 携导出与新指纹（同步方持久化
 * 指纹供下轮协商）。
 *
 * <p>指纹 = {@link SessionExportChecksum#contentFingerprint}（内容投影——剔除
 * exportedAtEpochMs）；匹配值异常形态 fail-open 走 EXPORTED（协商失败不给 304）。
 */
public final class SessionExportConditional {

    /** 协商结果状态。 */
    public enum Status { EXPORTED, UNCHANGED }

    /**
     * 协商结果（不可变）：UNCHANGED 时 {@code export} 为 null（payload 不外发——
     * 协商的价值所在）；EXPORTED 时携导出与最新内容指纹。
     */
    public record Result(Status status, SessionExport export, String contentFingerprint) {
    }

    private SessionExportConditional() {
    }

    /**
     * 条件导出：fresh 与 ifNoneMatch 内容指纹比较。
     *
     * @param fresh       本轮构建的导出（每次调用 exportedAt 不同——不影响指纹）
     * @param ifNoneMatch 上轮持久化的内容指纹（null/空/垃圾 = 无条件 EXPORTED）
     */
    public static Result exportIfChanged(SessionExport fresh, String ifNoneMatch) {
        if (fresh == null) {
            throw new IllegalArgumentException("fresh export 非空");
        }
        String fingerprint = SessionExportChecksum.contentFingerprint(fresh);
        if (ifNoneMatch != null && !ifNoneMatch.isBlank()
                && ifNoneMatch.trim().equals(fingerprint)) {
            return new Result(Status.UNCHANGED, null, fingerprint);
        }
        return new Result(Status.EXPORTED, fresh, fingerprint);
    }
}
