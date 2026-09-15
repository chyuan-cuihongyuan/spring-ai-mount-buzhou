package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 写偏斜检测（spec 1861 / T2923 / impl 1462）——数据库快照隔离 write
 * skew 异象（经典「值班医生」反例）：两事务**读集相交**（都读了支撑不变
 * 量的数据）而**写集不相交**（改不同的行）——写冲突检测放行（无同键
 * 竞争）、双方均可提交，但组合不变量被破（两个医生都看到对方在班而
 * 各自请假，班表空转）。可串行化才拦得住；快照隔离下要靠**显式冲突
 * 桌**或检测面。本检测面输出风险对清单——接线归宿主。
 *
 * <p>纯函数零状态、只检测不拦截。
 */
public final class WriteSkewDetector {

    private WriteSkewDetector() {
    }

    /** 事务事实契约：id 非空白、读写集非 null（可为空集）。 */
    public record Transaction(String id, Set<String> readSet, Set<String> writeSet) {

        public Transaction {
            if (id == null || id.isBlank() || readSet == null || writeSet == null) {
                throw new IllegalArgumentException(String.format(
                        "非法事务事实：id=%s（要求非空白且读写集非 null）", id));
            }
        }
    }

    /** 风险对：两个可提交但组合不变量可破的事务。 */
    public record SkewPair(String first, String second) {
    }

    /**
     * 双事务偏斜风险判定。契约：null 视为无风险（防御）；语义：双方读写
     * 集均非空 且 读集相交 且 写集不相交——写冲突检测拦不住的组合风险。
     */
    public static boolean skewRisk(Transaction a, Transaction b) {
        if (a == null || b == null) {
            return false;
        }
        boolean bothActive = !a.readSet().isEmpty() && !a.writeSet().isEmpty()
                && !b.readSet().isEmpty() && !b.writeSet().isEmpty();
        if (!bothActive) {
            return false;
        }
        boolean readsOverlap = a.readSet().stream().anyMatch(b.readSet()::contains);
        boolean writesDisjoint = a.writeSet().stream()
                .noneMatch(b.writeSet()::contains);
        return readsOverlap && writesDisjoint;
    }

    /**
     * 全对扫描：输出全部风险对（i&lt;j 入参序，确定性）。null 按空表。
     */
    public static List<SkewPair> scan(List<Transaction> transactions) {
        List<Transaction> window = transactions == null ? List.of() : transactions;
        List<SkewPair> risks = new ArrayList<>();
        for (int i = 0; i < window.size(); i++) {
            for (int j = i + 1; j < window.size(); j++) {
                if (skewRisk(window.get(i), window.get(j))) {
                    risks.add(new SkewPair(window.get(i).id(), window.get(j).id()));
                }
            }
        }
        return List.copyOf(risks);
    }
}
