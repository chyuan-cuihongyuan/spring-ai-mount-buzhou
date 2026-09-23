package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 尺寸分层合并挑选（spec 4013 / T6027 / impl 2114）——LSM 尺寸
 * 分层压实思想（Cassandra SizeTieredCompactionStrategy）：同量级
 * 小块合并成大块——**按尺寸分桶**（均值 ±50% 内视为同层）、桶员
 * ≥ minThreshold（Cassandra 默认 4）才成组、成组也只取
 * maxThreshold 个最小者（单次压实写放大封顶）；多桶竞选取员最多
 * 者（小文件优先清）。
 *
 * <p>「新旧混压」（把巨块反复重写）与「碎片堆积」（小块永不合并）
 * 两病的折中件——与 ChunkCompressionPolicy（块龄阈值）成压实
 * 双档按数据形态选型。纯裁决无 IO（压实动作归调用方）。
 */
public final class SizeTieredMergePicker {

    /** 候选块（id + 字节数）。 */
    public record Candidate(String id, long sizeBytes) {
    }

    private final int minThreshold;
    private final int maxThreshold;
    private final double bucketRadius;   // 同层判定：均值 ×(1/radius)–×radius

    /** 定构（2≤min≤max、radius>1 否则 fail-fast）。 */
    public SizeTieredMergePicker(int minThreshold, int maxThreshold, double bucketRadius) {
        if (minThreshold < 2 || maxThreshold < minThreshold) {
            throw new IllegalArgumentException("2≤min≤max 违反：" + minThreshold + "/" + maxThreshold);
        }
        if (bucketRadius <= 1.0) {
            throw new IllegalArgumentException("radius>1：" + bucketRadius);
        }
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.bucketRadius = bucketRadius;
    }

    /** 挑选成组（尺寸升序、并列 id 序；无合格组 → 空表）。 */
    public List<String> pickMergeGroup(List<Candidate> candidates) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates 非 null");
        }
        for (Candidate c : candidates) {
            if (c == null || c.id() == null || c.id().isEmpty() || c.sizeBytes() < 0) {
                throw new IllegalArgumentException("候选非空且 size≥0");
            }
        }
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingLong(Candidate::sizeBytes).thenComparing(Candidate::id));

        List<List<Candidate>> buckets = new ArrayList<>();
        List<Candidate> bucket = new ArrayList<>();
        double sum = 0;
        for (Candidate c : sorted) {
            if (bucket.isEmpty()) {
                bucket.add(c);
                sum = c.sizeBytes();
                continue;
            }
            double avg = sum / bucket.size();
            if (c.sizeBytes() >= avg / bucketRadius && c.sizeBytes() <= avg * bucketRadius) {
                bucket.add(c);
                sum += c.sizeBytes();
            } else {
                buckets.add(bucket);
                bucket = new ArrayList<>(List.of(c));
                sum = c.sizeBytes();
            }
        }
        if (!bucket.isEmpty()) {
            buckets.add(bucket);
        }

        List<Candidate> best = null;
        for (List<Candidate> b : buckets) {
            if (b.size() < minThreshold) {
                continue;
            }
            if (best == null || b.size() > best.size()) {
                best = b;   // 桶员最多者优先（先遇并列保持——确定性）
            }
        }
        if (best == null) {
            return List.of();
        }
        return best.stream().limit(maxThreshold).map(Candidate::id).toList();
    }

    /** minThreshold 读数。 */
    public int minThreshold() {
        return minThreshold;
    }

    /** maxThreshold 读数。 */
    public int maxThreshold() {
        return maxThreshold;
    }
}
