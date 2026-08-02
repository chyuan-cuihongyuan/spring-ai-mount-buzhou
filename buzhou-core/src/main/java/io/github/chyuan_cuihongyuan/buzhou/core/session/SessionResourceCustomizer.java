package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionResourceRegistry;

@FunctionalInterface
public interface SessionResourceCustomizer {

    void customize(SessionResourceRegistry registry, String appId, String agentName, String sessionId);
}
