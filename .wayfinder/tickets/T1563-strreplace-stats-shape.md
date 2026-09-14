---
id: T1563
title: str_replace 编辑判定读面（StrReplaceStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1561
created: 2026-09-15
---

## Question

J 会话第 54 轮：spill 域编辑工具的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（spill 域第二轴）：StrReplaceTool 六路径全部零计数——尤其 notFound（oldStr 未出现）与 ambiguous（oldStr 非唯一）是 Anthropic text editor str_replace 的两大经典失败模式，其分布直接反映模型编辑能力与提示词引导有效性。

形状裁决：`StrReplaceTool` 内静态 `AtomicLong` 七计数——attempts（入口）/ successes（替换成功）/ paramRejects（缺 newStr 与空 oldStr 两点合桶——同为参数校验）/ missingFileRejects（目标不存在）/ notFoundRejects（occurrences=0）/ ambiguousRejects（occurrences>1）/ failures（catch 兜底）；嵌套 `record StrReplaceStats` + `stats()` + `resetForTest()`。守恒 `attempts = successes + paramRejects + missingFileRejects + notFoundRejects + ambiguousRejects + failures`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：按 path 分桶（敏感面红线）；文件写入量计量（R46 write_file 已覆盖写侧字节口径）。
