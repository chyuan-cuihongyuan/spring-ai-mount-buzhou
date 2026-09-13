# 910 — AIMD 自适应批量 yml 装配

> 来源：I 会话第 11 轮 = effort #910（[T1271](../../.wayfinder/tickets/T1271-aimd-yml-shape.md) / [T1272](../../.wayfinder/tickets/T1272-aimd-yml-verify.md) / impl 663）。D/G 会话装配轮模式（spec 105 `include-types` 同法）。

## 背景

spec 907 的 `setAdaptiveBatchEnabled` 只有编程构造面——yml 声明式部署用户不可用。装配三层缝里补声明式一层。

## 目标

- `webhookEventForwarder` bean 构造点：Binder 直读 `buzhou.webhook.adaptive-batch`（boolean；缺省 false）→ `forwarder.setAdaptiveBatchEnabled(...)`；
- 缺省（无此键）行为逐字节不变；显式 false 与缺省同语义；
- spring-configuration-metadata 自动补全随 `@ConfigurationProperties` 侧无新增（env 直读键）——配置键入 config 键清单文档（README 配置矩阵若有 webhook 段则同步）。

## 测试

装配分支单测：env 带 true → `currentBatchSize()` 随批波动；缺省 → 恒 32。既有 forwarder 用例全绿。

## 兼容性

纯增量装配面；缺省逐字节不变。
