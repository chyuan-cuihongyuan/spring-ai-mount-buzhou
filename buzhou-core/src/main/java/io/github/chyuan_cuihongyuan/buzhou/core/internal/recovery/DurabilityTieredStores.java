package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.DurabilityTier;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeys;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 持久化强度分档的存储侧表达（spec「崩溃中轮次恢复 / 持久化强度三档」）。
 *
 * <p>档位是<b>存储实现侧的写缓冲策略</b>：编排方（记忆写路径）不按档位分支，由本装饰器
 * 包在 {@link BuzhouStores} 写路径上生效，不新增 SPI。三档语义（对标 LangGraph durability）：
 *
 * <ul>
 *   <li>{@code SYNC} —— 写直达底层（同步落盘后返回）：相邻步骤间崩溃至多丢在途那一步。</li>
 *   <li>{@code ASYNC} —— 写直达底层（内存/JDBC/Redis 后端的 append/put 本身即「shortly after
 *       持久」的语义边界）：默认档，吞吐优先 + 最终持久。</li>
 *   <li>{@code EXIT} —— 写仅入会话级缓冲、<b>读侧穿透合并</b>（本会话内 load/get 仍见自己的写），
 *       {@link #flush} 时才批量落底层：最高吞吐，崩溃丢整轮，由恢复语义兜底；
 *       flush 钩子供会话谢幕与 06 优雅停机 drain 调用。</li>
 * </ul>
 *
 * <p>契约语义由共享契约测试（{@code AbstractBuzhouStoresContractTest} 三档断言）锁定，
 * 内存 / JDBC / Redis 三后端继承同一装饰器、行为一致。
 */
public final class DurabilityTieredStores {

    private DurabilityTieredStores() {
    }

    /**
     * 按档位包装存储写路径。
     *
     * @param delegate 底层存储（任一后端）
     * @param tier     持久化强度档位
     * @return SYNC/ASYNC 返回原实例（写直达语义无需装饰）；EXIT 返回带写缓冲的包装实例
     */
    public static BuzhouStores wrap(BuzhouStores delegate, DurabilityTier tier) {
        if (tier != DurabilityTier.EXIT) {
            return delegate;
        }
        return new BuzhouStores(
                new ExitTierMessageStore(delegate.messageStore()),
                delegate.summaryStore(),
                new ExitTierSessionStateStore(delegate.sessionStateStore()),
                delegate.sessionLeaseStore(),
                delegate.observabilityStore(),
                delegate.unitOfWork());
    }

    /**
     * flush EXIT 档缓冲写入（幂等；非 EXIT 档包装或原实例为 no-op）。
     * 会话谢幕 / 06 优雅停机 drain 调用——受控停机不丢缓冲数据。
     */
    public static void flush(BuzhouStores stores) {
        if (stores.messageStore() instanceof ExitTierMessageStore exit) {
            exit.flush();
        }
        if (stores.sessionStateStore() instanceof ExitTierSessionStateStore exit) {
            exit.flush();
        }
    }

    /** EXIT 档消息存储：append 入缓冲、load 穿透合并、flush 批量落底层。 */
    static final class ExitTierMessageStore implements MessageStore {
        private final MessageStore delegate;
        private final ConcurrentHashMap<String, CopyOnWriteArrayList<BuzhouMessage>> buffer =
                new ConcurrentHashMap<>();

        ExitTierMessageStore(MessageStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
            buffer.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).addAll(messages);
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            List<BuzhouMessage> merged = new ArrayList<>(delegate.load(sessionId));
            merged.addAll(buffer.getOrDefault(sessionId, new CopyOnWriteArrayList<>()));
            merged.sort(Comparator.comparingInt(BuzhouMessage::turnSeq)
                    .thenComparingInt(BuzhouMessage::seqInTurn));
            return merged;
        }

        @Override
        public Optional<BuzhouMessage> findById(String messageId) {
            Optional<BuzhouMessage> persisted = delegate.findById(messageId);
            if (persisted.isPresent()) {
                return persisted;
            }
            return buffer.values().stream()
                    .flatMap(List::stream)
                    .filter(m -> m.id().equals(messageId))
                    .findFirst();
        }

        void flush() {
            buffer.forEach((sessionId, messages) -> {
                if (!messages.isEmpty()) {
                    delegate.append(sessionId, List.copyOf(messages));
                }
            });
            buffer.clear();
        }
    }

    /**
     * EXIT 档会话 state 存储：put 入缓冲、get/getAll 穿透合并、delete 双侧生效、flush 批量落底层。
     *
     * <p><b>去重记录例外（写直达）</b>：{@code dedup.} 前缀的幂等去重记录<b>不入缓冲</b>，
     * 所有读写直达底层——「工具已执行、结果未 append」的崩溃窗口里，去重记录是恰好一次语义的
     * 唯一凭证，若随缓冲在崩溃时丢失则效果恰好一次被静默打破；且 {@code putIfAbsent} 的原子性
     * 只有落到底层共享后端才对跨实例并发生效（每个会话包装实例各有私有缓冲）。
     */
    static final class ExitTierSessionStateStore implements SessionStateStore {
        private final SessionStateStore delegate;
        private final ConcurrentHashMap<String, ConcurrentHashMap<String, StateEntry>> buffer =
                new ConcurrentHashMap<>();

        ExitTierSessionStateStore(SessionStateStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void put(String sessionId, StateEntry entry) {
            if (isDedupKey(entry.key())) {
                delegate.put(sessionId, entry);
                return;
            }
            buffer.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(entry.key(), entry);
        }

        @Override
        public Optional<StateEntry> get(String sessionId, String key) {
            if (isDedupKey(key)) {
                return delegate.get(sessionId, key);
            }
            ConcurrentHashMap<String, StateEntry> session = buffer.get(sessionId);
            if (session != null && session.containsKey(key)) {
                return Optional.ofNullable(session.get(key));
            }
            return delegate.get(sessionId, key);
        }

        @Override
        public Map<String, StateEntry> getAll(String sessionId) {
            Map<String, StateEntry> merged = new java.util.HashMap<>(delegate.getAll(sessionId));
            ConcurrentHashMap<String, StateEntry> session = buffer.get(sessionId);
            if (session != null) {
                merged.putAll(session);
            }
            return Map.copyOf(merged);
        }

        @Override
        public void delete(String sessionId, String key) {
            if (isDedupKey(key)) {
                delegate.delete(sessionId, key);
                return;
            }
            ConcurrentHashMap<String, StateEntry> session = buffer.get(sessionId);
            if (session != null) {
                session.remove(key);
            }
            delegate.delete(sessionId, key);
        }

        @Override
        public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
            if (isDedupKey(key)) {
                return delegate.deleteIfValueMatches(sessionId, key, expectedValue);
            }
            ConcurrentHashMap<String, StateEntry> session = buffer.get(sessionId);
            if (session != null && session.containsKey(key)) {
                boolean[] removed = {false};
                session.computeIfPresent(key, (k, e) -> {
                    if (java.util.Objects.equals(e.value(), expectedValue)) {
                        removed[0] = true;
                        return null;
                    }
                    return e;
                });
                return removed[0];
            }
            return delegate.deleteIfValueMatches(sessionId, key, expectedValue);
        }

        @Override
        public boolean putIfAbsent(String sessionId, StateEntry entry) {
            if (isDedupKey(entry.key())) {
                // 去重记录的原子占位必须落在底层共享后端，跨实例并发才互斥
                return delegate.putIfAbsent(sessionId, entry);
            }
            ConcurrentHashMap<String, StateEntry> session =
                    buffer.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
            if (session.containsKey(entry.key())) {
                return false;
            }
            if (delegate.get(sessionId, entry.key()).isPresent()) {
                return false;
            }
            return session.putIfAbsent(entry.key(), entry) == null;
        }

        void flush() {
            buffer.forEach((sessionId, entries) -> entries.values()
                    .forEach(entry -> delegate.put(sessionId, entry)));
            buffer.clear();
        }

        /** 幂等去重记录键（{@link IdempotencyKeys#PREFIX}）——EXIT 档下写直达、不入缓冲。 */
        private static boolean isDedupKey(String key) {
            return key.startsWith(IdempotencyKeys.PREFIX);
        }
    }
}
