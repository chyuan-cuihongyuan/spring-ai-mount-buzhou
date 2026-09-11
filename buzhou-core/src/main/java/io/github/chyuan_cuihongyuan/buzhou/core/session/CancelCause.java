package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 取消原因枚举（spec 606 / T862，gRPC status codes 思想——取消的「谁发起、为什么」
 * 以稳定机器可读闭集传播，观测面不再只有 cancelMode 一个维度）。
 *
 * <p>既有 {@link #cancel()} / {@link #cancel(CancelMode)} 路径默认 {@link #USER}
 * （零行为变化）；停机排水传 {@link #SHUTDOWN_DRAIN}；DEADLINE / LEASE_LOST /
 * RUNAWAY 供对应机制发起取消时显式声明（当前各走异常路径，后续接入面见各 spec）。
 */
public enum CancelCause {

    /** 用户/调用方主动取消（默认口径——无法区分时的诚实缺省）。 */
    USER,

    /** 优雅停机排水取消（runtime shutdown 对在途 Turn 的强制收敛）。 */
    SHUTDOWN_DRAIN,

    /** 轮次预算（turnDeadline/loopTimeout）到期触发的取消。 */
    DEADLINE,

    /** 会话租约丢失（另一实例接管——本实例让位收敛）。 */
    LEASE_LOST,

    /** 失控检测硬顶终止（runaway 保护触发）。 */
    RUNAWAY
}
