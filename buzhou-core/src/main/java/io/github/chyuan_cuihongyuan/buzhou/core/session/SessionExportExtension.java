package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话导出扩展（spec 36 §A / T121 / impl-96）：模块把自有会话数据段并入
 * {@link SessionExport#extensions()}（key = 扩展名，value = JSON 字符串）——
 * core 三槽（messages/summary/state）之外的模块数据经此进可移植文档。
 *
 * <p>导出：非空段才入 extensions；导入：按新 sessionId 回放，异常只 WARN 不阻断
 * （扩展段失败不回滚已完成的三槽导入——最终一致口径）。
 *
 * @since 1.0.0
 */
public interface SessionExportExtension {

    /** 扩展名（extensions map 的 key；建议模块前缀如 {@code memory.facts}）。 */
    String name();

    /** 导出该会话的模块数据段（null/空 = 本次不携带）。 */
    String exportSegment(String sessionId);

    /** 导入回放（targetSessionId 为重映射后的新 Id）。 */
    void importSegment(String targetSessionId, String json);
}
