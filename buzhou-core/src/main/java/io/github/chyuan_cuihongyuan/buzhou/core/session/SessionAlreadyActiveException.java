package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

/**
 * 会话已被激活异常：lease 门拒绝并发接管（拿不到即拒绝）。
 * 携带 {@link ErrorCode#SESSION_ALREADY_ACTIVE}（不可重试——需 steal 或先关闭既有会话）。
 */
public class SessionAlreadyActiveException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    public SessionAlreadyActiveException(String sessionId) {
        super(ErrorCode.SESSION_ALREADY_ACTIVE, "Session already active: " + sessionId);
    }
}
