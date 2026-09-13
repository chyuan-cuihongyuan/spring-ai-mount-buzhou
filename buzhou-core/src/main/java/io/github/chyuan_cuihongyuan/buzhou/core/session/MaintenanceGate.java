package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

/**
 * 维护模式门（spec 205 / T577，K8s cordon 借鉴）：全局 volatile 开关——
 * 维护中 beforeTurn（order 10，见 {@link MaintenanceGateHook}）温和拒绝
 * （文案含 reason 与预计恢复时间）。维护序列：gate.begin 拒新 → 逐会话
 * drain（155）排存量 → 维护 → gate.end。纯内存（进程级——多实例同步留档）。
 *
 * <p>spec 1004 / T1459：闭窗历史读面（cordon/uncordon 事件史思想）——begin/end
 * 生命周期入有界环（新→旧），窗内拒绝数按窗分账；{@code history()} 供维护后
 * 复盘（何时、为何、多久、拒了多少）。begin/end 对外语义不变。
 */
public final class MaintenanceGate {

    /** 闭窗历史环容量（有界纪律）。 */
    static final int HISTORY_CAPACITY = 16;

    /** 维护窗口声明。 */
    public record Window(String reason, Instant expectedBackAt) {
    }

    /** 已闭维护窗（历史条目，不可变；仅含 begin→end 完整生命周期）。 */
    public record HistoryEntry(Window window, Instant beganAt, Instant endedAt, long refusals) {
    }

    private volatile Window window;
    private volatile Instant beganAt;
    private final LongAdder refusals = new LongAdder();
    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    /** 进入维护（reason 非空；expectedBackAt 可空=时间未知文案降级）。 */
    public void begin(String reason, Instant expectedBackAt) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("维护 reason 非空");
        }
        synchronized (history) {
            this.window = new Window(reason, expectedBackAt);
            this.beganAt = Instant.now();
            this.refusals.reset();
        }
    }

    /** 结束维护（幂等；活跃窗闭窗入史——非活跃时无痕）。 */
    public void end() {
        synchronized (history) {
            Window current = this.window;
            if (current == null) {
                return;
            }
            Instant started = this.beganAt == null ? Instant.now() : this.beganAt;
            long refusedTotal = refusals.sumThenReset();
            this.window = null;
            this.beganAt = null;
            history.addFirst(new HistoryEntry(current, started, Instant.now(), refusedTotal));
            while (history.size() > HISTORY_CAPACITY) {
                history.removeLast();
            }
        }
    }

    /** 是否维护中。 */
    public boolean isActive() {
        return window != null;
    }

    /** 当前窗口（无 = empty）。 */
    public Optional<Window> window() {
        return Optional.ofNullable(window);
    }

    /** 窗内拒绝计数（窗外调用无害无痕——Hook 每次温和拒绝同点补记）。 */
    void noteRefused() {
        if (window != null) {
            refusals.increment();
        }
    }

    /** 闭窗历史只读快照（新→旧；仅含 begin→end 完整生命周期的窗）。 */
    public List<HistoryEntry> history() {
        synchronized (history) {
            return List.copyOf(history);
        }
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
