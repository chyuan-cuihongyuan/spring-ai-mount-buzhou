package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 后台任务选主 SPI（spec 331 / T653，K8s leader election / etcd lease 借鉴）：
 * 同一 scope 同一时刻只有一个执行者——持有人持 TTL 租约，周期性
 * {@link #tryAcquireOrRenew()} 续期；空位时候选者获取；持有人主动
 * {@link #resign()} 让位（停机快速故障转移）。
 *
 * <p>围栏纪元（{@link Leadership#epoch()}）单调递增且跨重启不回退——
 * 旧持有人迟到续期被判失效不复活旧纪元（与 303 持久纪元同思想）。
 *
 * <p>消费方纪律（RetentionSweeper 先例）：调度周期先取续——非 leader
 * 跳过本周期（留计数）；取续抛异常 = 本周期跳过（失联宁可少做不可
 * 抢做——K8s 失租即停执行同语义）；手动触发入口不设门（运维按钮是
 * 人的决定）。
 */
public interface LeaderElector {

    /**
     * 尝试获取或续期领导权（幂等——已是 leader 则续 TTL 并返回原纪元；
     * 空位则获取并进入新纪元；他人持有则返回跟随态）。
     *
     * @throws RuntimeException 后端不可达等——调用方按「本周期不执行」处理
     */
    Leadership tryAcquireOrRenew();

    /** 主动让位（仅当前持有人有效；停机快速故障转移用）。 */
    void resign();

    /** 观测面：当前持有者与纪元（不尝试获取；空位时 holder 为 null、epoch 为 0）。 */
    default Leadership inspect() {
        return new Leadership(null, 0, false);
    }

    /**
     * 一次取/续/观测的结果。
     *
     * @param holder 持有人标识（inspect 空位时 null）
     * @param epoch  围栏纪元（获取时递增、续期不变；跨重启单调）
     * @param leader 本实例此刻是否持有
     */
    record Leadership(String holder, long epoch, boolean leader) {
    }
}
