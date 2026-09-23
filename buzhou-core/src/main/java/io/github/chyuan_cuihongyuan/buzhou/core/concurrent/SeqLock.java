package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * SeqLock 序号锁（spec 5007 / T6115 / impl 2158）——Linux 内核
 * seqlock 思想：序号奇偶标写入期（偶=稳定、奇=写入中）；
 * `writeBegin` 偶→奇（单写者 CAS 串行，并发写者失败退避）、
 * `writeEnd` 奇→偶；读者 `readBegin` 取序号（奇则重试）、读
 * 完 `readEnd(observed)` 校验序号未变且为偶——失败整读作废
 * 重试。读者零阻塞、无读者互斥、写者不被读者饿死——读写锁
 * （读者互斥/写者饿死）与全量加锁（读路径成本高）的病解。
 *
 * <p>与 TicketLock 同族不同面：写者公平互斥 vs 读者乐观重试。
 * 单写者口径（多写者仲裁不在本件）。
 */
public final class SeqLock {

    private final AtomicLong sequence = new AtomicLong();

    /**
     * 写入期开始（偶→奇；并发写者 CAS 失败自旋退避）。
     *
     * @return 写入期序号（奇）
     */
    public long writeBegin() {
        long current = sequence.get();
        while (current % 2 == 1 || !sequence.compareAndSet(current, current + 1)) {
            Thread.onSpinWait();
            current = sequence.get();
        }
        return current + 1;
    }

    /** 写入期结束（奇→偶；序号非本写入期 IAE fail-fast）。 */
    public void writeEnd(long writeSequence) {
        if (writeSequence % 2 != 1) {
            throw new IllegalArgumentException("writeEnd 需奇序号（写入期中）：" + writeSequence);
        }
        if (!sequence.compareAndSet(writeSequence, writeSequence + 1)) {
            throw new IllegalArgumentException("序号被他人推进（非本写入期）：" + writeSequence);
        }
    }

    /** 读取期开始（返回当前序号；奇=写入中，调用方应重试）。 */
    public long readBegin() {
        return sequence.get();
    }

    /** 读取期校验（序号未变且为偶 → 本次读取一致）。 */
    public boolean readEnd(long observed) {
        return observed % 2 == 0 && observed == sequence.get();
    }

    /** 当前序号读数。 */
    public long sequence() {
        return sequence.get();
    }

    /** 是否有写入期进行中。 */
    public boolean isWriting() {
        return sequence.get() % 2 == 1;
    }
}
