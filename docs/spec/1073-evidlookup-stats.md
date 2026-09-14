# 1073 — evidence_lookup 证据回查读面

> 来源：J 会话第 73 轮 = effort #1073（[T1601](../../.wayfinder/tickets/T1601-evidlookup-stats-shape.md) / [T1602](../../.wayfinder/tickets/T1602-evidlookup-stats-verify.md) / impl 825）。借鉴：Redis cache hit-rate（回查 miss 率高 = 引用与存储失配的第一信号）。memory/tool 域第二轴（R59 compact_now 前置）。

## Problem Statement

`EvidenceLookupTool.call()`（evidence_id 证据回查通道）全路径零计数——回查频次、未找到率、切片率不可见：**证据链引用有效性无对账**。模型反复按失效 id 回查（miss 高发）意味着上游证据引用与 MessageStore 生命周期失配；切片率高说明窗口与模型精读需求不匹配。

## 目标

- `EvidenceLookupTool` 增量（memory/tool，静态面）：五 `AtomicLong` 双守恒。
  - `calls`：call 入口（总桶）；`misses`：findById 未命中；
  - `hits`：命中（= completeReads + slicedReads 之和）；
  - `completeReads`（全文返回）/ `slicedReads`（带截断标记返回——切片率信号）。
- 嵌套 `record EvidenceLookupStats(...)` + `stats()` + `resetForTest()`。
- 双守恒恒等式：**calls = hits + misses**；**hits = completeReads + slicedReads**。

## 兼容性

纯增量读面：call() 返回语义、切片与截断标记逐位不变；静态面理由同 R46–R72 先例；无新配置项。

## Out of Scope

- 按 evidenceId 分桶（敏感面——红线纪律）。
- 字节数口径（R47 读侧已立，不重复）。
