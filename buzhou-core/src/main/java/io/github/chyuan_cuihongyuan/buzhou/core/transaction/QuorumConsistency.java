package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

/**
 * 法定人数一致性（spec 1876 / T2953 / impl 1477）——Dynamo/Cassandra 的
 * quorum 读写交集语义：N 副本下每次读写 R/W 份，{@code R+W > N} 则任意
 * 读集与任意写集必有公共副本——「读到最新写」有数学保证而非运气。交集
 * 数（overlap）、读写可容忍故障余量、一致性可用余量是同一笔账的三个读数。
 *
 * <p>纯函数零状态、确定性；只算不选副本（读修复/副本选择归存储层）。
 */
public final class QuorumConsistency {

    private QuorumConsistency() {
    }

    /**
     * 强一致判定：{@code R + W > N}——真则任意读集含最新写入的副本。
     * 契约：N ≥ 1、1 ≤ R ≤ N、1 ≤ W ≤ N（fail-fast）。
     */
    public static boolean strongConsistency(int replicas, int readQuorum,
                                            int writeQuorum) {
        validate(replicas, readQuorum, writeQuorum);
        return readQuorum + writeQuorum > replicas;
    }

    /**
     * 保证交集：任意读集与任意写集的公共副本数下界 = R+W−N，
     * 负值钳 0（0 = 交集无保证——弱一致）。
     */
    public static int overlapCount(int replicas, int readQuorum, int writeQuorum) {
        validate(replicas, readQuorum, writeQuorum);
        return Math.max(0, readQuorum + writeQuorum - replicas);
    }

    /**
     * 写侧可容忍故障数：N−W——还能照常收写的最大副本故障数。
     */
    public static int tolerableWriteFailures(int replicas, int writeQuorum) {
        validate(replicas, 1, writeQuorum);
        return replicas - writeQuorum;
    }

    /**
     * 读侧可容忍故障数：N−R——还能凑齐读法定人数的最大副本故障数。
     */
    public static int tolerableReadFailures(int replicas, int readQuorum) {
        validate(replicas, readQuorum, 1);
        return replicas - readQuorum;
    }

    /**
     * 一致性可用余量：读写可容忍故障的较小者——挂到这个数，强一致
     * 读写仍同时可服务；超过则强一致或可用必弃其一。
     */
    public static int consistentAvailability(int replicas, int readQuorum,
                                            int writeQuorum) {
        validate(replicas, readQuorum, writeQuorum);
        return Math.min(replicas - readQuorum, replicas - writeQuorum);
    }

    private static void validate(int replicas, int readQuorum, int writeQuorum) {
        if (replicas < 1) {
            throw new IllegalArgumentException("replicas 不能小于 1：" + replicas);
        }
        if (readQuorum < 1 || readQuorum > replicas) {
            throw new IllegalArgumentException(String.format(
                    "readQuorum 须在 [1, %d]：%d", replicas, readQuorum));
        }
        if (writeQuorum < 1 || writeQuorum > replicas) {
            throw new IllegalArgumentException(String.format(
                    "writeQuorum 须在 [1, %d]：%d", replicas, writeQuorum));
        }
    }
}
