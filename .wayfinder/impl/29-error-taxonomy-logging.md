# 29 — 横切 · 异常分类体系 + 错误码 + 日志基线

**What to build:** 生产可诊断的分类地基：统一 BuzhouException 体系（可重试/不可重试/致命分类 + 结构化 ErrorCode），工具错误反馈识别从字符串前缀改为结构化标记（对外消息词汇不变），关键路径日志基线（SLF4J 占位符风格）。后续所有切片引用此分类。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouException 基类 + RetryCategory{RETRYABLE,NON_RETRYABLE,FATAL} + ErrorCode 枚举（sealed 风格，api 包，Javadoc）
- [x] 既有三个领域异常（SandboxViolation/LeaseLost/SessionAlreadyActive）纳入体系
- [x] isErrorFeedback 字符串前缀识别改为结构化标记（反馈对象携带类型；对外文本兼容）
- [x] 日志基线落地：静默吞异常点（DbPolicyConfigProvider、DefaultFactStore、closeAll 等）补 WARN/ERROR + 上下文
- [x] 新增 QuotaExceededException / BuzhouDataCorruptionException 占位（供后续片使用）
- [x] 既有测试全绿；新增分类单元测试（类别判定）

> 落地说明（2026-08-14，buzhou-core）：分类体系落在新 api 包 `io.github.chyuan_cuihongyuan.buzhou.core.error`
> （仓内本无 core.api 包，新包即事实上的错误分类 api 面）。ErrorCode 为枚举（每码带 RetryCategory +
> 默认消息模板），BuzhouException 不 sealed（buzhou-spill 的壳异常跨模块继承需开放）。
> isErrorFeedback 迁移：标记单一事实源收敛到 `exec/ToolFeedbackType` 枚举（两档 EXECUTION_FAILURE /
> VALIDATION_FAILURE），格式化（ToolErrorFeedback.MARKER / ToolValidationFeedback.MARKER）与识别
> （HarnessToolCallingManager.responsesForModel）共用枚举常量；对外消息词汇不变，旧前缀文本仍被识别。
> 日志基线：DbPolicyConfigProvider 轮询 WARN + 连续失败计数（阈值 3 升 ERROR，不改轮询节奏）、
> DefaultFactStore 序列化/反序列化退化 WARN、SessionResourceRegistry.closeAll 收集全部失败（suppressed
> 附加，不静默吞）、DanglingCallRepairer 重放失败 WARN、FileSandbox 沙箱拒绝 WARN。slf4j-api 依赖入
> buzhou-core pom（版本随 Boot BOM）。新增测试：ErrorTaxonomyTest / ToolFeedbackTypeTest /
> SessionResourceRegistryTest（方法命名 should…_when…）。
