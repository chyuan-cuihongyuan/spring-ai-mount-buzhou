package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Fencing Token 世代令牌护栏（spec 5004 / T6109 / impl 2155）——
 * Chubby fencing token 思想（Kleppmann DDIA 界碑）：锁每次
 * 持有权取得（含易主/重入）发**严格递增** token；存储侧
 * `tryWrite` 三态裁决——token=当前已发 ACCEPT、token&lt;当前
 * STALE_TOKEN（旧持有者苏醒写被物理拦截）、token&gt;当前
 * UNKNOWN_TOKEN（未发过的令牌诚实拒）、未发令 NO_LOCK。
 * 锁服务与存储两侧状态脱节（GC 停顿/网络分区下旧持有者续写）
 * 的病解；release 不重置世代——fencing 跨释放持久，旧令牌
 * 永不复用；嵌套 {@link Verdict} 不另立面。
 *
 * <p>与 CommitGraph（世代号血缘剪枝）同族不同面；与
 * SequenceFence（webhook 序栅）不同面。
 */
public final class FencingTokenGuard {

    private final Map<String, Long> issued = new HashMap<>();
    private final Map<String, String> holders = new HashMap<>();

    /**
     * 取得持有权并发令（严格递增；同 holder 重入亦发新代）。
     *
     * @return 新 fencing token
     */
    public long acquire(String lockId, String holderId) {
        checkId(lockId, "lockId");
        checkId(holderId, "holderId");
        long token = issued.merge(lockId, 1L, Long::sum);
        holders.put(lockId, holderId);
        return token;
    }

    /** 释放持有权（世代不重置——fencing 跨释放持久）。 */
    public void release(String lockId, String holderId) {
        checkId(lockId, "lockId");
        checkId(holderId, "holderId");
        if (!holderId.equals(holders.get(lockId))) {
            throw new IllegalArgumentException("非当前持有者：" + lockId + "/" + holderId);
        }
        holders.remove(lockId);
    }

    /** 写侧守卫裁决（三态 + 无锁态）。 */
    public Verdict tryWrite(String lockId, long token) {
        checkId(lockId, "lockId");
        Long current = issued.get(lockId);
        if (current == null) {
            return Verdict.NO_LOCK;
        }
        if (token == current) {
            return Verdict.ACCEPTED;
        }
        return token < current ? Verdict.STALE_TOKEN : Verdict.UNKNOWN_TOKEN;
    }

    /** 当前最大已发 token 读数（未发令返回 0）。 */
    public long currentToken(String lockId) {
        checkId(lockId, "lockId");
        return issued.getOrDefault(lockId, 0L);
    }

    /** 当前持有者读数（无持有者返回 null）。 */
    public String holderOf(String lockId) {
        checkId(lockId, "lockId");
        return holders.get(lockId);
    }

    private static void checkId(String id, String name) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(name + " 非空");
        }
    }

    /** 写守卫裁决四态。 */
    public enum Verdict {
        ACCEPTED, STALE_TOKEN, UNKNOWN_TOKEN, NO_LOCK
    }
}
