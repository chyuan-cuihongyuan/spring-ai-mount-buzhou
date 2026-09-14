# 1047 — read_file 读量水位与拒绝分桶读面

> 来源：J 会话第 47 轮 = effort #1047（[T1549](../../.wayfinder/tickets/T1549-readfile-stats-shape.md) / [T1550](../../.wayfinder/tickets/T1550-readfile-stats-verify.md) / impl 799）。借鉴：Datadog DogStatsD read/write 对称计量（读写两轴同构分桶，轴间可比）。spec 1046 Out of Scope 预告的读侧分轴顺延；与 R46 写侧分轴（那轴是 write_file 写入分布，本轴是 read_file 读取分布）。

## Problem Statement

`ReadFileTool`（spec 06 / impl-49 读入上限预检）的拒绝路径——文件不存在、超 8MB、沙箱拒绝、异常兜底——当前只返回失败字符串：**读吞吐与拒绝原因分布不可见**。宿主无法回答"读取成功率多少、失败集中在不存在还是超限"；Spill 前的读流量水位也无从对账。

## 目标

- `ReadFileTool` 增量（tools/file，静态面）：六 `AtomicLong`。
  - `attempts`：call() 入口计数（总桶）；
  - `reads`：成功整读计数；`bytesRead`：成功读取的 UTF-8 字节累计；
  - `notFileRejects`（不存在/非普通文件）/ `oversizeRejects`（超 8MB 预检）/ `failures`（catch 兜底，含沙箱拒绝）三个拒绝分桶。
- 嵌套 `record ReadFileStats(long attempts, long reads, long bytesRead, long notFileRejects, long oversizeRejects, long failures)`（`totalRejects()` 派生）+ `stats()` + `resetForTest()`。
- 守恒恒等式：**attempts = reads + notFileRejects + oversizeRejects + failures**（每入口恰落一桶）。
- 与 R46 写侧轴间可比：bytesRead/bytesWritten 同为 UTF-8 字节口径。

## 兼容性

纯增量读面：call() 返回语义、上限预检与异常路径逐位不变；无新配置项。

## Out of Scope

- offset/limit 范围读取（瘦 Schema 原则，归 read_range——spec 06 推演 #2 不变）。
- 按 path 分桶（路径含敏感面——红线纪律）。
