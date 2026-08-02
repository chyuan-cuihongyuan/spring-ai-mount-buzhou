package io.github.chyuan_cuihongyuan.buzhou.core.session;

@FunctionalInterface
public interface SessionEventListener {

    void onEvent(SessionEvent event);
}
