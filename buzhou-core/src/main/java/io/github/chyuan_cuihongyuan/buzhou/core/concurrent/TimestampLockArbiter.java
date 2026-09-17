package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;

/**
 * 时间戳锁仲裁器（spec 3040 / T5081 / impl 2040）——数据库死锁
 * 预防思想（Rosenkrantz 1978 wait-die / wound-wait）：年长者
 * （时间戳小）有特权——wait-die：**老等少死**（老者等锁、少者
 * 请求被老者持有的锁即自杀重启）；wound-wait：**老伤少等**（老者
 * 直接夺锁伤持有者、少者等待）。两模式都保证等待图无环——死锁
 * 预防而非检测（TarjanSccFinder 是事后指认，本件事前不发生）。
 * 多 agent 资源占用的死锁免疫仲裁件（纯仲裁无阻塞——等待/重启
 * 动作归调用方）。
 *
 * <p>年长序：(timestamp, txnId) 字典序小者老（同钟并列可全序）；
 * 同持有者重复请求幂等 GRANTED。
 */
public final class TimestampLockArbiter {

    /** 预防模式。 */
    public enum Mode {
        /** 老等少死：年长者等待，年幼者请求即死（重启）。 */
        WAIT_DIE,
        /** 老伤少等：年长者夺锁伤持有者，年幼者等待。 */
        WOUND_WAIT
    }

    /** 仲裁结果。 */
    public enum Decision {
        /** 获锁（空闲/幂等/伤占）。 */
        GRANTED,
        /** 请求者应等待（年长序上占优）。 */
        WAIT,
        /** 请求者应放弃重启（wait-die 年幼者）。 */
        REQUESTER_ABORTS
    }

    /** 仲裁输出（被伤方 txnId——wound 场景的受害者，无则 −1）。 */
    public record Outcome(Decision decision, long woundedTxn) {
        static final long NONE = -1;
    }

    private record Holder(long txnId, long timestamp) {
    }

    private final Mode mode;
    private final Map<String, Holder> holders = new HashMap<>();

    /** 模式定构。 */
    public TimestampLockArbiter(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode 非空");
        }
        this.mode = mode;
    }

    /** 仲裁请求：空闲即授；同持有者幂等；冲突按模式裁决。 */
    public Outcome request(long txnId, long timestamp, String resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource 非空");
        }
        Holder holder = holders.get(resource);
        if (holder == null) {
            holders.put(resource, new Holder(txnId, timestamp));
            return new Outcome(Decision.GRANTED, Outcome.NONE);
        }
        if (holder.txnId() == txnId) {
            return new Outcome(Decision.GRANTED, Outcome.NONE);
        }
        boolean requesterOlder = requesterOlder(timestamp, txnId, holder);
        if (mode == Mode.WAIT_DIE) {
            return requesterOlder
                    ? new Outcome(Decision.WAIT, Outcome.NONE)
                    : new Outcome(Decision.REQUESTER_ABORTS, txnId);
        }
        if (requesterOlder) {
            holders.put(resource, new Holder(txnId, timestamp));
            return new Outcome(Decision.GRANTED, holder.txnId());
        }
        return new Outcome(Decision.WAIT, Outcome.NONE);
    }

    /** 释放（非持有者释放无副作用）。 */
    public void release(long txnId, String resource) {
        Holder holder = holders.get(resource);
        if (holder != null && holder.txnId() == txnId) {
            holders.remove(resource);
        }
    }

    /** 持有判定。 */
    public boolean holds(long txnId, String resource) {
        Holder holder = holders.get(resource);
        return holder != null && holder.txnId() == txnId;
    }

    /** 当前持有者 txnId（空闲 −1）。 */
    public long holderOf(String resource) {
        Holder holder = holders.get(resource);
        return holder == null ? Outcome.NONE : holder.txnId();
    }

    private static boolean requesterOlder(long requesterTs, long requesterId, Holder holder) {
        if (requesterTs != holder.timestamp()) {
            return requesterTs < holder.timestamp();
        }
        return requesterId < holder.txnId();
    }
}
