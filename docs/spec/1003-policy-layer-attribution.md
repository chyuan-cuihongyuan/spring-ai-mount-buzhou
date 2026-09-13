# 1003 — 策略层级归属读面

> 来源：J 会话第 4 轮 = effort #1003（[T1457](../../.wayfinder/tickets/T1457-policy-layer-attribution-shape.md) / [T1458](../../.wayfinder/tickets/T1458-policy-layer-attribution-verify.md) / impl 756）。借鉴：Spring Boot config insights / [layer attribution](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)（生效配置「来自哪一层」的归因显形）。

## Problem Statement

四层覆盖模型（spec 02：默认 < yml 全局 < 绑定级 < 工具级）的前三层由 `LayeredPolicy.get` 解析——binding > yml > defaults 首中即胜。但**生效值来自哪一层**不可见：配置排查要肉眼对三层，配置医生类工具（spec 107 config-doctor）无归因 API 可依。

## 目标

- 新公共 record `PolicyLayerAttribution`（core.policy，api 面）：`record(dottedKey, Layer, value)`；嵌套枚举 `Layer = DEFAULTS | YML | BINDING | ABSENT`（ABSENT⇔value 恒 null——构造期校验双拒）。
- `LayeredPolicy.getAttributed(dottedKey)`：与 `get` 同序同判（binding > yml > defaults 首中即胜），另带归属层；`get()` 重构为归因路径薄封装（逐位零行为变化）。
- 工具级（第四层）不经本读面（`ToolPolicyMatcher` 匹配决策见 spec 1000）。

## 兼容性

纯增量：get/getMap 返回值逐位不变；无新配置项、无计数器（纯函数诊断面——无生产调用方则计数为假面，诚实不设）。

## Out of Scope

- getMap 深合并的逐叶归因（合并语义下「来源层」需逐键判定，复杂度另行立项）。
- 配置医生自动消费（消费侧另立，本轮只供 API）。
