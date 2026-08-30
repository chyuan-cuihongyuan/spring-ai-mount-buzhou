package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.Optional;

public interface SessionStateHandle {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value);

    void delete(String key);

    /**
     * 条件写（CAS，spec 56 §B / T250）：仅当当前 value 与 {@code expectedValue} 相等时以
     * {@code update} 覆写（字符串化口径与 {@link #put} 一致），返回是否成功；
     * {@code expectedValue == null} 表示键不存在才写。原子计数（配额等）依赖本方法 +
     * 失败重读重试循环；跨实例原子性由底层 store 覆写承诺（见
     * {@code SessionStateStore#compareAndSwap} 分层诚实边界）。
     *
     * <p>默认实现 get+比对+put <b>非原子</b>（仅单实例语义；既有实现二进制兼容）——
     * 真原子由 {@code HookEnvironment} 透传 store CAS 覆写提供。
     */
    default boolean compareAndSwap(String key, String expectedValue, Object update) {
        String current = get(key, String.class).orElse(null);
        if (!java.util.Objects.equals(current, expectedValue)) {
            return false;
        }
        put(key, update);
        return true;
    }
}
