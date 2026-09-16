package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动豁免窗追踪（spec 2013 / T3127 / impl 1564）——K8s startup probe
 * 思想：慢启动实例在豁免窗内的失败**不计故障账**（防冷启动抖动被误
 * 判为不健康），首次成功即毕业（后续失败恢复全额计账）——豁免是给
 * 冷启动的宽容，不是给僵尸的免死牌。
 *
 * <p>synchronized 小临界区；时间由调用方传入（确定性可回放）。
 */
public final class StartupGraceTracker {

    /** 实例启动锚（豁免窗起点）。 */
    private record Anchor(long startedAtMillis, boolean graduated) {
    }

    private final long graceMillis;
    private final Map<String, Anchor> anchors = new HashMap<>();
    private long exemptedFailures;
    private long countedFailures;
    private long graduations;

    /** 契约：graceMillis &gt; 0（fail-fast）。 */
    public StartupGraceTracker(long graceMillis) {
        if (graceMillis <= 0) {
            throw new IllegalArgumentException("graceMillis 须 > 0：" + graceMillis);
        }
        this.graceMillis = graceMillis;
    }

    /** 锚定实例启动（豁免窗自此刻起；重复锚定取最新——重启重锚）。 */
    public synchronized void begin(String instanceId, long nowMillis) {
        requireId(instanceId);
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        anchors.put(instanceId, new Anchor(nowMillis, false));
    }

    /**
     * 报告失败并自动分流：已毕业或豁免窗外 → 计账（counted）；
     * 未毕业且窗内 → 豁免（exempted，不计故障账）。未锚定实例一律
     * 计账（无豁免资格）。
     */
    public synchronized void reportFailure(String instanceId, long nowMillis) {
        requireId(instanceId);
        Anchor anchor = anchors.get(instanceId);
        if (anchor == null || anchor.graduated()
                || nowMillis - anchor.startedAtMillis() >= graceMillis) {
            countedFailures++;
            return;
        }
        exemptedFailures++;
    }

    /** 首次成功即毕业（幂等——后续失败全额计账）。未锚定实例无操作。 */
    public synchronized void graduate(String instanceId) {
        requireId(instanceId);
        Anchor anchor = anchors.get(instanceId);
        if (anchor != null && !anchor.graduated()) {
            anchors.put(instanceId, new Anchor(anchor.startedAtMillis(), true));
            graduations++;
        }
    }

    /** 豁免窗内未毕业的实例数（正在受宽容的实例面）。 */
    public synchronized int activeGraces(long nowMillis) {
        int active = 0;
        for (Anchor a : anchors.values()) {
            if (!a.graduated() && nowMillis - a.startedAtMillis() < graceMillis) {
                active++;
            }
        }
        return active;
    }

    /** 账面快照：豁免失败/计账失败/毕业数。 */
    public synchronized GraceStats stats() {
        return new GraceStats(exemptedFailures, countedFailures, graduations);
    }

    private static void requireId(String instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId 不能为 null");
        }
    }

    /** 豁免账快照。 */
    public record GraceStats(long exemptedFailures, long countedFailures, long graduations) {
    }
}
