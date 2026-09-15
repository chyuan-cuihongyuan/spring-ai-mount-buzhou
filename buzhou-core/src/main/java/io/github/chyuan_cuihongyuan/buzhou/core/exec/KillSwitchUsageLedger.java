package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 工具开关使用台账（L 会话 1700 系 R16 = effort #1715 / spec 1715 /
 * 票 T2631 + T2632 / impl 1315）——Unleash / LaunchDarkly 式特性开关
 * 审计思想：kill switch 的每次扳动（杀/放）都该留痕——谁在何时以何因
 * 杀了什么工具、杀了多久；无痕开关是事故追溯的死穴。
 *
 * <p>实例面有界台账（默认 128 条，满则逐出最旧——ring 纪律）：
 * `recordKill(tool, reason, atMillis)` / `recordRestore(tool, atMillis)`
 * 配对累计封禁时长。纯读面 opt-in，不改 {@link ToolKillSwitchHook} 判定。
 *
 * @since 1.0.0
 */
public final class KillSwitchUsageLedger {

    /** 默认台账容量。 */
    public static final int DEFAULT_CAPACITY = 128;

    /**
     * @param tool     工具名
     * @param reason   扳动原因（宿主声明）
     * @param atMillis 扳动时刻
     */
    public record FlipEntry(String tool, String reason, long atMillis) {
    }

    private final int capacity;
    private final Deque<FlipEntry> entries;
    private final List<FlipEntry> openKills = new ArrayList<>();
    private long killedDurationMillis;
    private long killCount;
    private long restoreCount;

    /** 默认容量。 */
    public KillSwitchUsageLedger() {
        this(DEFAULT_CAPACITY);
    }

    /** 自定义容量（&lt;1 按默认）。 */
    public KillSwitchUsageLedger(int capacity) {
        this.capacity = capacity < 1 ? DEFAULT_CAPACITY : capacity;
        this.entries = new ArrayDeque<>(this.capacity);
    }

    /** 记一次杀（封禁生效）。 */
    public synchronized void recordKill(String tool, String reason, long atMillis) {
        append(new FlipEntry(tool, reason, atMillis));
        killCount++;
        openKills.add(new FlipEntry(tool, reason, atMillis));
    }

    /** 记一次放（与该工具最近一次未配对杀配对，累计封禁时长）。 */
    public synchronized void recordRestore(String tool, long atMillis) {
        append(new FlipEntry(tool, "restore", atMillis));
        restoreCount++;
        for (int i = openKills.size() - 1; i >= 0; i--) {
            if (openKills.get(i).tool().equals(tool)) {
                killedDurationMillis += Math.max(0, atMillis - openKills.remove(i).atMillis());
                return;
            }
        }
    }

    private void append(FlipEntry entry) {
        if (entries.size() >= capacity) {
            entries.pollFirst();
        }
        entries.addLast(entry);
    }

    /** 台账只读快照（旧→新）。 */
    public synchronized List<FlipEntry> entries() {
        return List.copyOf(entries);
    }

    /** 累计封禁时长（已配对部分）。 */
    public synchronized long killedDurationMillis() {
        return killedDurationMillis;
    }

    /** 杀/放计数。 */
    public synchronized long[] counters() {
        return new long[]{killCount, restoreCount};
    }
}
