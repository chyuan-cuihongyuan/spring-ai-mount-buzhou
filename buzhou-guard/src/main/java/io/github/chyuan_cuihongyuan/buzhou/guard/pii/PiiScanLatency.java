package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.ArrayList;
import java.util.List;

/**
 * PII 扫描耗时分位读面（L 会话 1700 系 R39 = effort #1738 / spec 1738 /
 * 票 T2677 + T2678 / impl 1338）——Envoy per-filter 计时思想：PII 扫描
 * （{@link PiiDetector}）在每个请求路径上跑，扫描本身吃掉多少延迟须有账
 * ——「安全不能比漏洞更慢」。与 {@link PiiHitStats}（命中面）互补。
 *
 * <p>实例面线程安全：`record(scanMillis)`（负值忽略）累积 +`report()`
 * 吐样本/中位/p95（最近秩）/最大（无样本哨兵 −1）。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class PiiScanLatency {

    private final Object lock = new Object();
    private final List<Long> scans = new ArrayList<>();

    /** 记一次扫描耗时（负值忽略）。 */
    public void record(long scanMillis) {
        if (scanMillis < 0) {
            return;
        }
        synchronized (lock) {
            scans.add(scanMillis);
        }
    }

    /**
     * @param samples     样本数
     * @param medianMillis 中位（无样本哨兵 −1）
     * @param p95Millis   p95（最近秩）
     * @param maxMillis   最大
     */
    public record LatencyReport(int samples, long medianMillis,
                                long p95Millis, long maxMillis) {
    }

    /** 快照。 */
    public LatencyReport report() {
        List<Long> sorted;
        synchronized (lock) {
            if (scans.isEmpty()) {
                return new LatencyReport(0, -1L, -1L, -1L);
            }
            sorted = new ArrayList<>(scans);
        }
        sorted.sort(Long::compare);
        int n = sorted.size();
        long median = n % 2 == 1
                ? sorted.get(n / 2)
                : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
        int p95Index = Math.max(0, (int) Math.ceil(0.95 * n) - 1);
        return new LatencyReport(n, median, sorted.get(p95Index), sorted.get(n - 1));
    }
}
