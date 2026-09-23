package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * QUIC 反放大窗（spec 4019 / T6039 / impl 2120）——未验证对端防
 * 放大思想（QUIC RFC 9000 §8.1）：对端地址未验证（未证明能收其
 * 回包）前，服务端发送量不得超**已接收量的 k 倍**（k=3）——
 * 防被诱骗当反射器打第三方（IP 伪造源放大攻击 DDoS 面）；地址
 * 验证通过即解除窗（恢复全速）。
 *
 * <p>信用窗模型：收包入账 ×factor、发包扣减、超信用 fail-fast
 （MUST NOT 语义）；初始授信覆盖握手首包（未收任何包也要能回）。
 * 与 HierarchicalTokenBucket（稳态限速）互补：本件是**未验证
 * 对端**的临时放大闸。纯逻辑无 IO。
 */
public final class QuicAmplificationWindow {

    private final int factor;
    private long credit;
    private boolean validated;

    /** 定构（factor≥1、initialCredit≥0 否则 fail-fast）。 */
    public QuicAmplificationWindow(int factor, long initialCredit) {
        if (factor < 1 || initialCredit < 0) {
            throw new IllegalArgumentException("factor≥1 / initialCredit≥0：" + factor + "/" + initialCredit);
        }
        this.factor = factor;
        this.credit = initialCredit;
    }

    /** 收包入账（信用 += bytes×factor）。 */
    public void onReceived(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("bytes>0：" + bytes);
        }
        credit += bytes * factor;
    }

    /** 发包扣减（验证前超信用 fail-fast——MUST NOT 放大语义；验证后窗解除不设限）。 */
    public void onSent(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("bytes>0：" + bytes);
        }
        if (!validated) {
            if (bytes > credit) {
                throw new IllegalStateException("超反放大窗：欲发 " + bytes + " / 信用 " + credit);
            }
            credit -= bytes;
        }
    }

    /** 可发判定（验证后恒真）。 */
    public boolean canSend(long bytes) {
        return validated || bytes <= credit;
    }

    /** 地址验证通过——解除窗（此后不受限）。 */
    public void validateAddress() {
        validated = true;
    }

    /** 剩余信用读数（验证后 Long.MAX_VALUE 语义面）。 */
    public long credit() {
        return validated ? Long.MAX_VALUE : credit;
    }

    /** 验证态读数。 */
    public boolean validated() {
        return validated;
    }
}
