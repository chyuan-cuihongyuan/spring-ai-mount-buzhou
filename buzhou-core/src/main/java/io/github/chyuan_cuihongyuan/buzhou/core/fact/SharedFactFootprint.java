package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 共享事实足迹读数（spec 741 / T1084，717/724 治理思想的 owner 维度合流）：
 * 按 owner 聚合事实数、永生事实（ttl=null）数——「谁的事实在无限堆积」的
 * 归因面。值不读取（隐私口径——只看键/owner/ttl 元数据）。
 *
 * <p>纯函数：调用方供事实快照（export/readable 投影）。
 */
public final class SharedFactFootprint {

    /** 单 owner 行（facts 降序；同数 owner 字典序稳定）。 */
    public record Row(String owner, long facts, long eternal) {
    }

    /** 不可变报告。 */
    public record Report(List<Row> rows, long totalFacts, long eternalFacts) {
    }

    private SharedFactFootprint() {
    }

    /** 归因（null fail-fast；空表 = 零行）。 */
    public static Report analyze(List<SharedFact> facts) {
        Objects.requireNonNull(facts, "facts");
        Map<String, long[]> byOwner = new LinkedHashMap<>(); // [0]=facts [1]=eternal
        long total = 0;
        long eternal = 0;
        for (SharedFact fact : facts) {
            if (fact == null) {
                continue;
            }
            total++;
            boolean noTtl = fact.ttl() == null;
            if (noTtl) {
                eternal++;
            }
            long[] agg = byOwner.computeIfAbsent(fact.owner(), k -> new long[2]);
            agg[0]++;
            if (noTtl) {
                agg[1]++;
            }
        }
        List<Row> rows = new ArrayList<>(byOwner.size());
        byOwner.forEach((owner, agg) -> rows.add(new Row(owner, agg[0], agg[1])));
        rows.sort(Comparator.comparingLong(Row::facts).reversed().thenComparing(Row::owner));
        return new Report(List.copyOf(rows), total, eternal);
    }
}
