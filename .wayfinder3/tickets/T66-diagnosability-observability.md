---
id: T66
title: 横切 · 可诊断性——错误分类、日志、泄漏检测、健康与指标
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

生产不可诊断如何修复？需裁决：① BuzhouException 体系（统一基类 + 可重试/不可重试分类 + 结构化错误码枚举；isErrorFeedback 字符串前缀匹配替换为结构化标记）；② 关键路径日志基线（WARN/ERROR 覆盖哪些点，占位符风格统一）；③ LeakDetector（四级采样 + 出租时长阈值 + LeakListener，Netty/HikariCP 语义——挂在 SessionResourceRegistry/句柄/租约上）；④ HealthIndicator（三机制，禁用报 UNKNOWN 不拖垮聚合）+ @Endpoint(id="buzhou") 只读快照 + MeterBinder optional 探测（指标命名 buzhou.<mech>.<测量>、tag 值有界）；⑤ 与 buzhou-observability 模块的边界（不动该模块，指标桥接经 MeterBinder bean 自动绑定）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §cross-11**：BuzhouException sealed 体系 + RetryCategory{RETRYABLE,NON_RETRYABLE,FATAL} + ErrorCode 枚举；错误反馈识别改结构化标记（消息词汇不变）；日志基线（停机超时/续租失败/背压丢弃/清理失败/审计降级/沙箱拒绝/数据损坏）；ResourceLeakDetector（四级 + 1/128 采样 + 出租时长阈值 + LeakListener）挂会话资源/句柄/租约；每机制 HealthIndicator（禁用报 UNKNOWN）+ @Endpoint(id="buzhou") + MeterBinder conditionalOnClass（未装 no-op）；指标 buzhou.<mech>.<测量>、tag 有界。观测模块零改动。
