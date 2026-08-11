package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 崩溃恢复语义分档（spec「崩溃中轮次恢复」/ CONTEXT「自动重驱动」）。
 *
 * <p>进程在轮次中途崩溃后，新实例经租约交接获取同一会话、加载并经悬空修复历史后，
 * 按本档位决定后续动作：
 *
 * <ul>
 *   <li>{@link #VOID}（轮次作废，默认）—— 修复历史后<b>等用户下一次输入</b>，不擅自续跑。
 *       现状语义的明确化 + 事件化，safe-by-default。</li>
 *   <li>{@link #AUTO_RESUME}（自动重驱动，opt-in）—— 修复历史后，若历史结尾为「被中断轮次」
 *       （无终结性助手回复），<b>无需用户输入</b>重新发起模型调用续跑该轮。
 *       续跑对副作用安全由幂等去重保证；崩溃循环由硬顶兜底。</li>
 * </ul>
 */
public enum ResumeStrategy {
    VOID,
    AUTO_RESUME
}
