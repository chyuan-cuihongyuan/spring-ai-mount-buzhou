package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话 fork 监听器（spec 26 / T105 / impl-80）：fork 复制完成后回调——store 外的
 * 会话关联数据（如 spill 证据引用登记）在此时接入 fork 语义。
 *
 * <p>回调在 fork 主路径同步执行；抛异常只 WARN 不使 fork 失败（复制已完成的会话
 * 不因旁路登记失败而回滚——降级为「无引用登记」的安全态）。
 */
@FunctionalInterface
public interface SessionForkListener {

    /**
     * @param sourceSessionId 源会话（不动）
     * @param newSessionId    fork 新会话（历史已复制完成）
     */
    void onForked(String sourceSessionId, String newSessionId);
}
