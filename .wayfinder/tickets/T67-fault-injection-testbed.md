---
id: T67
title: 横切 · 故障注入与韧性测试基建
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

韧性如何被证明？需裁决：① FaultInjectingToolCallback 形状（delay/failRate/hangForever/leakResource/cancelMidFlight，装饰 ToolCallback，Kafka Trogdor 进程内语义）；② 韧性场景矩阵（挂起工具→deadline 兜底、慢监听→背压、续租失败→LeaseLost 中止、写失败→降级语义、脏数据→隔离、停机排空→graceful）；③ ApplicationContextRunner 测试矩阵（auto-config 条件验证：有/无 micrometer、属性组合）；④ Toxiproxy 网络级注入是否本轮引入（默认否，注记）；⑤ 长运行泄漏 soakan 测试形态（有限时间内的小型循环压力）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §cross-13 + Testing Decisions**：FaultInjectingToolCallback 随 core test-jar（delay/failRate/hangForever/leakResource/cancelMidFlight）；韧性场景矩阵 14 项（挂起→deadline 兜底、慢监听→丢弃可见、坏监听→隔离、steal→LeaseLost、写失败双策略、脏 JSON 隔离、停机排空、熔断半开、MySQL 二启、迁移基线、链篡改、密钥轮换、配额拒绝、ApplicationContextRunner 装配矩阵）；Toxiproxy 不引入（注记）；soakan 以 examples gated 测试承载。
