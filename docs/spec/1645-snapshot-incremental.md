# 1645 · API 快照增量再生 + 同文件并行冲突化解

> 来源：N 会话 R46（effort #1645 / T2441–T2442 / impl 1198）。

## Solution

- 快照增量：R27 全量再生后新增的公开类型（GradientLimiterHolder 等）经
  隔离 worktree `-am` 再生（主工作区并行半成品挡路）+ 同步入档。
- 并行冲突化解：GuardModule 的 `dangerousTools()` 读数被 N/M 两会话同时添加
  （同文本双插/签名重复编译错）——保留 M 系版本（spec 1508 注释完整、字段链
  自洽），撤 N 方插入。共用工作区的同文件并发编辑是本流程的已知风险面。

## Out of Scope

- 快照再生的自动化（每轮触发成本高——R27/R46 两点增量够用）。
