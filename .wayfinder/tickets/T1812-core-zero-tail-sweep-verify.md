---
id: T1812
title: core 零覆盖尾巴清扫验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1811
created: 2026-09-15
---

## Question

R4 补测后：AttachmentRenderer / CommandOutcome 是否清零？收紧判据（miss≥1）下 core 是否无未入档残留？core 全量测试是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，隔离 worktree 全量 test + JaCoCo 复扫）：

1. **两靶点清零**：AttachmentRenderer（mis 6→0）/ CommandOutcome（mis 4→0）——default 截断合同与 success 谓词矩阵全执行。
2. **收紧判据下残留恰 1 项且已入档**：`BuzhouCoreAutoConfiguration$SmartLifecycle` 匿名类（mis=2，装配期样板）——与 R1 豁免台账一致，无未入档残留。复核另浮出 SnapshotMessage（mis=2）→ 归 R5 补测（T1813，收敛态 2→1）。
3. **全量绿**：core 2653 用例 0 失败 0 错误（隔离 worktree，含 T1815 加固后的并发压测——护栏生效，同环境曾挂死 109+ CPU 分钟的两跑非确定性不再复现）。
4. 主代码零变化；唯一主代码注释更正（T1808，R2）已独立 commit。
