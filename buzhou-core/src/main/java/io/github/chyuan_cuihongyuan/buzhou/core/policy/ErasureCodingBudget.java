package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * 纠删码冗余预算（spec 1881 / T2963 / impl 1482）——MinIO/Ceph 的
 * EC(k, m) 语义：k 数据分片 + m 校验分片构成一个条带。可丢 m 片
 * 仍可重建；修复一块坏盘需读 k 片（读放大 k 倍）；容量可用率
 * k/(k+m)——同容忍度下比多副本省容量。冗余三读数事前可算。
 *
 * <p>纯计算零状态；不做实际分片/重建（归存储层）。
 */
public final class ErasureCodingBudget {

    private ErasureCodingBudget() {
    }

    /**
     * 容量可用率：k/(k+m)——原始容量中存有效数据的比例（三副本
     * 对照 = EC(1,2) 的 1/3）。契约：data ≥ 1、parity ≥ 1
     * （零冗余非法——丢一片即数据丢失，fail-fast）。
     */
    public static double usableRatio(int data, int parity) {
        validate(data, parity);
        return (double) data / (data + parity);
    }

    /**
     * 可容忍故障数：= m——条带内丢到 m 片仍可重建（再多即数据丢失）。
     */
    public static int tolerableFailures(int data, int parity) {
        validate(data, parity);
        return parity;
    }

    /**
     * 修复读放大：= k——重建一片坏盘数据需读出的分片数（重建窗口
     * 网络流量是坏盘数据的 k 倍）。
     */
    public static int repairReads(int data) {
        if (data < 1) {
            throw new IllegalArgumentException("data 不能小于 1：" + data);
        }
        return data;
    }

    /**
     * 部署盘数下限：k + m。
     */
    public static int totalShards(int data, int parity) {
        validate(data, parity);
        return data + parity;
    }

    private static void validate(int data, int parity) {
        if (data < 1) {
            throw new IllegalArgumentException("data 不能小于 1：" + data);
        }
        if (parity < 1) {
            throw new IllegalArgumentException(
                    "parity 不能小于 1（零冗余丢一片即丢数据）：" + parity);
        }
    }
}
