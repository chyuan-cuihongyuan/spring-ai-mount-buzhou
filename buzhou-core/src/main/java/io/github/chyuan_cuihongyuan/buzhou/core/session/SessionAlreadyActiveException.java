package io.github.chyuan_cuihongyuan.buzhou.core.session;

public class SessionAlreadyActiveException extends RuntimeException {

    public SessionAlreadyActiveException(String sessionId) {
        super("Session already active: " + sessionId);
    }
}
