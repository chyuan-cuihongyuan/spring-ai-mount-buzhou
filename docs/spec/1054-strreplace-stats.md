# 1054 — str_replace 编辑判定读面

> 来源：J 会话第 54 轮 = effort #1054（[T1563](../../.wayfinder/tickets/T1563-strreplace-stats-shape.md) / [T1564](../../.wayfinder/tickets/T1564-strreplace-stats-verify.md) / impl 806）。借鉴：Anthropic text editor str_replace（notFound/ambiguous 两大经典失败模式的分布是编辑可学习性第一信号）。spill 域第二轴：R53 逐出判定 / R54 编辑判定。

## Problem Statement

`StrReplaceTool.call()`（写侧长内容姊妹工具）的全部路径——参数缺失、目标文件不存在、oldStr 未出现（notFound）、oldStr 非唯一（ambiguous）、替换成功、异常兜底——当前只返回字符串：**编辑成败与失败模式分布不可见**。宿主无法回答"模型编辑成功率多少、失败集中在没找准锚点（notFound）还是锚点歧义（ambiguous）"——这两者恰是提示词引导（要求带足够上下文）能否生效的直接信号。

## 目标

- `StrReplaceTool` 增量（spill，静态面）：七 `AtomicLong`。
  - `attempts`：call 入口（总桶）；`successes`：替换成功；
  - `paramRejects`（缺 newStr / 空 oldStr 两点合桶）/ `missingFileRejects` / `notFoundRejects`（occurrences=0）/ `ambiguousRejects`（occurrences&gt;1）/ `failures`（catch 兜底）五个拒绝桶。
- 嵌套 `record StrReplaceStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**attempts = successes + 五拒绝桶之和**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、唯一性判定与文件替换语义逐位不变；静态面理由同 R46–R53 先例；无新配置项。

## Out of Scope

- 按 path 分桶（敏感面——红线纪律）。
- 写入字节口径（R46 write_file 已覆盖写侧水位）。
