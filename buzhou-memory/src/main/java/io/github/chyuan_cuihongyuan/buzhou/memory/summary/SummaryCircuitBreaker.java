package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SummaryCircuitBreaker {

    private final int failureThreshold;
    private final Map<String, AtomicInteger> failuresBySession = new ConcurrentHashMap<>();

    public SummaryCircuitBreaker(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public boolean allows(String sessionId) {
        return failuresBySession.computeIfAbsent(sessionId, k -> new AtomicInteger()).get()
                < failureThreshold;
    }

    public void onSuccess(String sessionId) {
        failuresBySession.computeIfAbsent(sessionId, k -> new AtomicInteger()).set(0);
    }

    public void onFailure(String sessionId) {
        failuresBySession.computeIfAbsent(sessionId, k -> new AtomicInteger()).incrementAndGet();
    }
}
