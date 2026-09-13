---
id: T1503
title: 事实注入覆盖读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 27 轮：事实注入覆盖读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 27 轮 = effort #1026 / spec 1026 / impl 779）：缺口成立——FactAttachmentRenderer（spec 07 事实注入渲染器）两 render 重载全程零计数：渲染几次、注入几条事实、超限省略几条不可见（省略清单虽附尾在文本里但无量化水位）——max-inject-chars 配置是否过紧无据。落点 buzhou-guard fact 包：实例级 renders/factsInjected/factsOmitted 三 AtomicLong + 嵌套 record `FactInjectStats` + `stats()`；两参 render 委托三参实现（输出恒等——2 参=无上限语义）；空仓渲染返回 empty 不计。实例级；嵌套类型不动 API 快照。
