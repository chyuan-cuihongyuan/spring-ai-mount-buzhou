---
id: T1583
title: 读侧 Spotlighting 包裹判定读面（SpotlightStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1581
created: 2026-09-15
---

## Question

J 会话第 64 轮：guard/inject 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SpotlightHook（读侧 Spotlighting：外部数据回灌 prompt 前包裹标记，MSRC 间接注入防御）afterTool 四路径全部零计数——包裹率与幂等跳过分布不可见；「外部输出多大比例被聚光灯防护覆盖」是注入防御面的核心对账（OWASP LLM01 spotlighting 采用率思想）。

形状裁决：`SpotlightHook` 内静态 `AtomicLong` 五计数——invocations（afterTool 入口）/ wrapped（实际包裹）/ alreadyWrappedSkips（已包裹幂等跳过——readback 切片再入场景）/ noticeSkips（拦截告示跳过）/ errorSkips（error 或 null 结果跳过）；嵌套 `record SpotlightStats` + `stats()` + `resetForTest()`。守恒 `invocations = wrapped + alreadyWrappedSkips + noticeSkips + errorSkips`（每入口恰落一桶）。静态面理由同族先例；afterTool 返回与改写语义逐位不变。

Out of scope：按 content 来源分桶（内容敏感面）；标记字符密度统计（wrap 参数是配置面）。
