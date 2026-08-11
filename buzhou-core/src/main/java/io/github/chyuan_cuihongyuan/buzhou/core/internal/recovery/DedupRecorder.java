package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 幂等去重记录（spec「幂等三件套 ③ 去重记录」/ CONTEXT「幂等键」）。
 *
 * <p>复用既有 per-session 存储 SPI（{@link SessionStateStore}）+ 其原子 put-if-absent 语义，
 * 不新增持久化 SPI。记录生命周期：
 *
 * <ol>
 *   <li><b>reserve</b>（调用前）：以幂等键原子 put-if-absent 一条 pending 记录——并发/多实例
 *       同键只有一个 reserve 成功获得执行权。</li>
 *   <li><b>fill</b>（调用成功后）：持有者回填结果。「工具已执行、消息未 append」的崩溃窗口里，
 *       去重记录已捕获结果——恢复/重试命中时直接返回首次结果、不重执行
 *       （at-least-once 调用 + 去重 = 效果恰好一次）。</li>
 *   <li><b>release</b>（调用失败/超时/取消）：删除 pending 记录，允许后续重试重新 reserve。</li>
 * </ol>
 *
 * <p>value 编码：{@code P} = pending；{@code F<结果原文>} = 已回填。{@code F} 前缀把「回填了
 * 空串结果」与 pending 区分开。
 */
public final class DedupRecorder {

    /** pending 标记值（未回填）。 */
    public static final String PENDING_VALUE = "P";
    /** 已回填值前缀。 */
    public static final String FILLED_PREFIX = "F";
    /** 去重记录生产者标记。 */
    public static final String PRODUCER = "dedup";
    /** 去重记录不归属具体轮次（崩溃恢复后跨轮命中），createdTurn 统一占位值。 */
    private static final int RECORD_TURN = 0;
    /** 等待回填的轮询间隔。 */
    private static final long POLL_INTERVAL_MILLIS = 50L;

    private final SessionStateStore stateStore;

    public DedupRecorder(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 原子 reserve 一条 pending 记录。
     *
     * @return {@code true} 当且仅当本次成功占位（获得执行权）；{@code false} 表示同键已存在（命中去重）
     */
    public boolean reserve(String sessionId, String key) {
        return stateStore.putIfAbsent(sessionId,
                new StateEntry(key, PENDING_VALUE, PRODUCER, RECORD_TURN, null, Instant.now()));
    }

    /** 调用成功后回填结果（持有者覆写自己的 pending 记录）。 */
    public void fill(String sessionId, String key, String result) {
        stateStore.put(sessionId,
                new StateEntry(key, FILLED_PREFIX + result, PRODUCER, RECORD_TURN, null, Instant.now()));
    }

    /** 调用失败/超时/取消时释放 pending 记录，允许后续重试重新 reserve。 */
    public void release(String sessionId, String key) {
        stateStore.deleteIfValueMatches(sessionId, key, PENDING_VALUE);
    }

    /**
     * 查询已回填结果。
     *
     * @return 已回填时返回结果原文（可能为空串）；pending 或不存在时返回 {@link Optional#empty()}
     */
    public Optional<String> filledResult(String sessionId, String key) {
        return stateStore.get(sessionId, key)
                .map(StateEntry::value)
                .flatMap(DedupRecorder::decodeFilled);
    }

    /**
     * 有界等待同键记录被回填（并发同键调用场景：另一在途执行持有者回填后直接取其结果）。
     *
     * @param timeout 等待上限（与工具超时同口径）
     * @return 等待期内回填则返回结果；超时仍 pending / 记录消失返回 {@link Optional#empty()}
     */
    public Optional<String> awaitFilled(String sessionId, String key, Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            Optional<StateEntry> entry = stateStore.get(sessionId, key);
            if (entry.isEmpty()) {
                return Optional.empty();
            }
            Optional<String> filled = decodeFilled(entry.get().value());
            if (filled.isPresent()) {
                return filled;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** 解码已回填值：{@code F<结果>} → 结果原文；pending / 其他形态 → empty。 */
    private static Optional<String> decodeFilled(String value) {
        return value.startsWith(FILLED_PREFIX)
                ? Optional.of(value.substring(FILLED_PREFIX.length()))
                : Optional.empty();
    }
}
