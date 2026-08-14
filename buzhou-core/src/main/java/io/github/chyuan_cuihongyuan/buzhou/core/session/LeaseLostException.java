package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

/**
 * 会话租约丢失异常：租约被他方 steal / 过期不可再取时中止本地写入（双主窗口零脏写）。
 * 携带 {@link ErrorCode#LEASE_LOST}（不可重试——本地继续写即脏数据，需重新争抢租约后另行发起）。
 */
public class LeaseLostException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    public LeaseLostException(String sessionId) {
        super(ErrorCode.LEASE_LOST, "Session lease lost: " + sessionId);
    }
}
