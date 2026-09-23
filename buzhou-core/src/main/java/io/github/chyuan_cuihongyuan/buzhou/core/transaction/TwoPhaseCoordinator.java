package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Two-Phase Commit 协调器（spec 5016 / T6133 / impl 2167）——
 * 两阶段提交状态机：`begin` → PREPARING；`votePrepare` 收集
 * 投票（全部 yes → PREPARED，任一 no → ABORTED——一票否决）；
 * `commit` 仅自 PREPARED → COMMITTED；`abort` 自
 * PREPARING/PREPARED → ABORTED。跨资源自行提交（部分提交
 * ——原子性撕裂）与无阶段约束（ABORTED 后仍可 COMMIT——
 * 非法迁移静默通过）的病解；非法迁移/未知参与者 IAE
 * fail-fast；嵌套 {@link Phase} 枚举不另立面。
 *
 * <p>与 FencingTokenGuard（S5）互补：锁安全守卫 vs 原子提交
 * 状态机。
 */
public final class TwoPhaseCoordinator {

    /** 事务阶段。 */
    public enum Phase {
        PREPARING, PREPARED, COMMITTED, ABORTED
    }

    private final Map<String, Phase> phases = new HashMap<>();
    private final Map<String, Set<String>> participants = new HashMap<>();
    private final Map<String, Set<String>> voted = new HashMap<>();

    /** 开启事务（重复 id fail-fast）。 */
    public void begin(String txId, Set<String> participantNames) {
        if (txId == null || txId.isEmpty() || participantNames == null || participantNames.isEmpty()) {
            throw new IllegalArgumentException("txId 与参与者集非空");
        }
        if (phases.containsKey(txId)) {
            throw new IllegalArgumentException("重复事务：" + txId);
        }
        phases.put(txId, Phase.PREPARING);
        participants.put(txId, new HashSet<>(participantNames));
        voted.put(txId, new HashSet<>());
    }

    /**
     * 参与者投票（全 yes → PREPARED；任一 no → ABORTED）。
     *
     * @return 投票后的阶段
     */
    public Phase votePrepare(String txId, String participant, boolean yes) {
        Phase phase = phaseOf(txId);
        if (phase != Phase.PREPARING) {
            throw new IllegalArgumentException("非投票期：" + txId + "/" + phase);
        }
        Set<String> voters = participants.get(txId);
        if (!voters.contains(participant)) {
            throw new IllegalArgumentException("未知参与者：" + txId + "/" + participant);
        }
        Set<String> txVoted = voted.get(txId);
        if (!txVoted.add(participant)) {
            throw new IllegalArgumentException("重复投票：" + txId + "/" + participant);
        }
        if (!yes) {
            phases.put(txId, Phase.ABORTED);
            return Phase.ABORTED;   // 一票否决
        }
        if (txVoted.size() == voters.size()) {
            phases.put(txId, Phase.PREPARED);
            return Phase.PREPARED;
        }
        return Phase.PREPARING;
    }

    /** 提交（仅自 PREPARED；非法迁移 IAE）。 */
    public Phase commit(String txId) {
        Phase phase = phaseOf(txId);
        if (phase != Phase.PREPARED) {
            throw new IllegalArgumentException("commit 仅自 PREPARED：" + txId + "/" + phase);
        }
        phases.put(txId, Phase.COMMITTED);
        return Phase.COMMITTED;
    }

    /** 中止（自 PREPARING/PREPARED；非法迁移 IAE）。 */
    public Phase abort(String txId) {
        Phase phase = phaseOf(txId);
        if (phase != Phase.PREPARING && phase != Phase.PREPARED) {
            throw new IllegalArgumentException("abort 仅自 PREPARING/PREPARED：" + txId + "/" + phase);
        }
        phases.put(txId, Phase.ABORTED);
        return Phase.ABORTED;
    }

    /** 阶段读数（未知事务 fail-fast）。 */
    public Phase phaseOf(String txId) {
        Phase phase = phases.get(txId);
        if (phase == null) {
            throw new IllegalArgumentException("未知事务：" + txId);
        }
        return phase;
    }

    /** 已投票参与者读数（未知事务 fail-fast）。 */
    public Set<String> votedOf(String txId) {
        phaseOf(txId);
        return Collections.unmodifiableSet(new HashSet<>(voted.get(txId)));
    }
}
