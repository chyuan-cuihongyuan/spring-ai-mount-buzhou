package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

/**
 * 跨 store 会话迁移器（spec 38 §B / T136 / impl-109）：JDBC→Redis / 反向 / →内存的
 * 会话级搬迁——复用两条成熟管线（exportSession 全量导出 + importSession 重映射导入），
 * 不引入第三套数据通路。轻量工具（非自动服务）：调用方显式逐会话迁移。
 *
 * <p>语义：默认新 sessionId 重映射（跨环境 Id 撞车防护）；keepIds=true 沿用
 * importSession 的冲突 fail-fast。指标 {@code buzhou.session.migrations}。
 */
public final class SessionMigrator {

    private SessionMigrator() {
    }

    /**
     * 迁移单个会话。
     *
     * @param source   源 runtime（导出侧）
     * @param target   目标 runtime（导入侧；store 形态可不同）
     * @param sessionId 源会话
     * @param keepIds  true 保留原 Id（目标已存在消息时 fail-fast）
     * @return 目标会话 Id（默认重映射后的新 Id）
     */
    public static String migrate(AgentRuntime source, AgentRuntime target,
            String sessionId, boolean keepIds) {
        SessionExport export = source.exportSession(sessionId);
        String targetId = target.importSession(export, keepIds);
        BuzhouMetricsHolder.metrics().counter("buzhou.session.migrations");
        return targetId;
    }
}
