package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话导入失败（spec 28 / T107 / impl-82）：keepIds 下目标会话已存在消息（Id 冲突
 * fail-fast，绝不静默覆盖），或导出文档非法（格式/版本/结构损坏）。
 *
 * @since 1.0.0
 */
public class SessionImportException extends RuntimeException {

    public SessionImportException(String message) {
        super(message);
    }

    public SessionImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
