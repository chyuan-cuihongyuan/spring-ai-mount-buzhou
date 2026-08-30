package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.Optional;

/**
 * 维护模式门（spec 205 / T577，K8s cordon 借鉴）：全局 volatile 开关——
 * 维护中 beforeTurn（order 10，见 {@link MaintenanceGateHook}）温和拒绝
 * （文案含 reason 与预计恢复时间）。维护序列：gate.begin 拒新 → 逐会话
 * drain（155）排存量 → 维护 → gate.end。纯内存（进程级——多实例同步留档）。
 */
public final class MaintenanceGate {

    /** 维护窗口声明。 */
    public record Window(String reason, Instant expectedBackAt) {
    }

    private volatile Window window;

    /** 进入维护（reason 非空；expectedBackAt 可空=时间未知文案降级）。 */
    public void begin(String reason, Instant expectedBackAt) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("维护 reason 非空");
        }
        this.window = new Window(reason, expectedBackAt);
    }

    /** 结束维护（幂等）。 */
    public void end() {
        this.window = null;
    }

    /** 是否维护中。 */
    public boolean isActive() {
        return window != null;
    }

    /** 当前窗口（无 = empty）。 */
    public Optional<Window> window() {
        return Optional.ofNullable(window);
    }

    /** 拦截文案（维护中才有意义；恢复时间未知时降级）。 */
    public String refusalMessage() {
        Window current = window;
        String back = current == null || current.expectedBackAt() == null
                ? "预计尽快恢复" : "预计 " + current.expectedBackAt() + " 恢复";
        return "系统计划维护中（" + (current == null ? "" : current.reason()) + "；"
                + back + "）——新会话与新一轮暂时不可用，请稍后再试";
    }
}
