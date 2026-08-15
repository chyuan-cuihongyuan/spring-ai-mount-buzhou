package io.github.chyuan_cuihongyuan.buzhou.core.error;

/**
 * 结构化错误码（spec 13 §cross-11 / ticket 29）：每个错误码绑定一个 {@link RetryCategory}
 * 与默认消息模板，是全仓异常分类的单一事实源——后续切片（租约续租、存储降级、配额、停机排空）
 * 抛出的领域异常一律携带本枚举，告警与策略按 {@code retryCategory} 自动化分诊。
 *
 * <p>命名与覆盖范围以既有 throw 点盘点为准（core fs/session/exec + spec 13 规划中的
 * QUOTA_EXCEEDED / DATA_CORRUPTION / STORE_WRITE_FAILED / SHUTDOWN_INTERRUPTED）。
 * 增补错误码时必须同步给出类别与模板，不允许出现无分类错误码。
 */
public enum ErrorCode {

    // ---- RETRYABLE：瞬态故障，重试可能成功 ----

    /** 工具执行超时（外层 join 超时 / Turn Deadline 兜底）。 */
    TIMEOUT(RetryCategory.RETRYABLE, "操作超时"),

    /** 存储写入失败（瞬态抖动；后续切片配 FAIL_TURN / DEGRADE 策略）。 */
    STORE_WRITE_FAILED(RetryCategory.RETRYABLE, "存储写入失败"),

    /** 进程停机中断（排空超时被硬截断；停机窗口结束后可重新发起）。 */
    SHUTDOWN_INTERRUPTED(RetryCategory.RETRYABLE, "操作因进程停机被中断"),

    /** 工具执行失败（「错误即反馈」通道语义：模型修正后可重试）。 */
    TOOL_EXECUTION_FAILED(RetryCategory.RETRYABLE, "工具执行失败"),

    /** 溢出存储读写失败（spec 50 §A / T178：磁盘 IO 瞬态故障，重试可能成功）。 */
    SPILL_IO_FAILED(RetryCategory.RETRYABLE, "溢出存储读写失败"),

    /** 存储读取失败（spec 50 §A / T178：读取路径瞬态故障；读降级策略前的原始错误面）。 */
    STORE_READ_FAILED(RetryCategory.RETRYABLE, "存储读取失败"),

    // ---- NON_RETRYABLE：输入或状态被拒，重试必然再失败 ----

    /** 会话租约丢失（被他方 steal / 过期；本地须立即中止写入防双主脏写）。 */
    LEASE_LOST(RetryCategory.NON_RETRYABLE, "会话租约已丢失"),

    /** 会话已被激活（lease 门拒绝并发接管；需 steal 或先关闭既有会话）。 */
    SESSION_ALREADY_ACTIVE(RetryCategory.NON_RETRYABLE, "会话已被激活"),

    /** 会话已关闭（关闭后拒绝 chat / 注册资源等生命周期误用）。 */
    SESSION_CLOSED(RetryCategory.NON_RETRYABLE, "会话已关闭"),

    /** 沙箱违规（路径越界 / 解析失败；同一路径重试必然再被拒）。 */
    SANDBOX_VIOLATION(RetryCategory.NON_RETRYABLE, "沙箱边界违规"),

    /** 配额超额（noeviction 语义：明确拒绝而非静默丢弃；需释放或提升配额）。 */
    QUOTA_EXCEEDED(RetryCategory.NON_RETRYABLE, "配额已超额"),

    /** 参数校验失败（schema 校验未过；原样重试必再失败，需修正入参）。 */
    ARGS_VALIDATION_FAILED(RetryCategory.NON_RETRYABLE, "参数校验失败"),

    /** 结构化输出解析失败（REASK 一次后仍不合规；spec 19 / impl-62）。 */
    STRUCTURED_OUTPUT_FAILED(RetryCategory.NON_RETRYABLE, "结构化输出解析失败"),

    /** 会话单飞闸拒绝（同会话已有在途轮次；spec 40 §B / impl-123——并发轮次由「未定义」转「确定拒绝」）。 */
    TURN_IN_FLIGHT(RetryCategory.NON_RETRYABLE, "会话已有在途轮次"),

    /** 技能管理操作非法（状态冲突 / 依赖未装配；spec 50 §A / T178）。 */
    SKILL_OPERATION_INVALID(RetryCategory.NON_RETRYABLE, "技能管理操作非法"),

    // ---- FATAL：环境或数据根因，需人工介入 ----

    /** 数据损坏（单条记录无法解析 / 链校验断点；跳过计数并告警，需人工修复）。 */
    DATA_CORRUPTION(RetryCategory.FATAL, "数据已损坏"),

    /** 配置非法（启动期校验失败 / 配置组合冲突；需修正配置或代码）。 */
    CONFIG_INVALID(RetryCategory.FATAL, "配置非法");

    private final RetryCategory retryCategory;
    private final String defaultMessageTemplate;

    ErrorCode(RetryCategory retryCategory, String defaultMessageTemplate) {
        this.retryCategory = retryCategory;
        this.defaultMessageTemplate = defaultMessageTemplate;
    }

    /** 本错误码的重试分类（告警与自动化策略按此分诊）。 */
    public RetryCategory retryCategory() {
        return retryCategory;
    }

    /** 默认消息模板（领域异常未提供具体消息时的兜底文案）。 */
    public String defaultMessageTemplate() {
        return defaultMessageTemplate;
    }
}
