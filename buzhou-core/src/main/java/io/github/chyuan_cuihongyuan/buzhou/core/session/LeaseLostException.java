package io.github.chyuan_cuihongyuan.buzhou.core.session;

public class LeaseLostException extends RuntimeException {

    public LeaseLostException(String sessionId) {
        super("Session lease lost: " + sessionId);
    }
}
