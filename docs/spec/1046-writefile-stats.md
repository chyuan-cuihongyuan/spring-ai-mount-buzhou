# 1046 — write_file 写入量水位与拒绝分桶读面

> 来源：J 会话第 46 轮 = effort #1046（[T1547](../../.wayfinder/tickets/T1547-writefile-stats-shape.md) / [T1548](../../.wayfinder/tickets/T1548-writefile-stats-verify.md) / impl 798）。借鉴：Sentry discarded events（被丢弃事件按原因分桶显形，而非一个总数吞掉所有失败）+ Dropwizard Meter（字节吞吐计量）。与 R45 沙箱判定分轴（那轴是路径判定侧，本轴是写入工具侧的成功/拒绝分布）。

## Problem Statement

`WriteFileTool`（spec 24 写侧长内容 / impl-49 上限与原子替换 / impl-698 noclobber）的全部拒绝路径——缺 content、超 8MB、noclobber 拒绝、沙箱拒绝、异常兜底——当前只返回一句失败字符串：**写入吞吐与拒绝原因分布不可见**。宿主无法回答"写盘成功率多少、拒绝都发生在哪类原因、累计写了多少字节"；noclobber 误开导致全量拒绝这类系统性故障完全静默。

## 目标

- `WriteFileTool` 增量（tools/file，静态面）：七 `AtomicLong`。
  - `attempts`：call() 入口计数（总桶）；
  - `writes`：成功写入计数；`bytesWritten`：成功写入的 UTF-8 字节累计；
  - `paramRejects`（缺 content）/ `oversizeRejects`（超 8MB）/ `noclobberRejects`（noclobber 拒绝）/ `failures`（catch 兜底，含沙箱拒绝）四个拒绝分桶。
- 嵌套 `record WriteFileStats(long attempts, long writes, long bytesWritten, long paramRejects, long oversizeRejects, long noclobberRejects, long failures)` + `stats()` 只读快照 + `resetForTest()`。
- 守恒恒等式：**attempts = writes + paramRejects + oversizeRejects + noclobberRejects + failures**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、异常路径、noclobber/上限判定逐位不变；静态面理由同 FileSandbox.stats 先例（工具实例由装配层新建，宿主/测试读面绕开实例引用）；无新配置项。

## Out of Scope

- contentPath 收到但未剥离的 WARNING 计数（Hook 未装配告警已属可观测，留后续）。
- ReadFileTool 读侧计量（另轮分轴）。
- 按 path 分桶（路径含敏感面——红线纪律）。
