# Wayfinder Map — Buzhou 干跑拦截/执行计划（effort #323，C 会话第 24 轮）

> C 会话第 24 轮。HITL（300）管「关键动作等人批」，混沌（235）管「演练
> 韧性」——都缺 Terraform 最核心的一对概念：**plan 与 apply 分离**。让
> agent 在只读演练中先产出「将要调用什么」的计划面，人工过目后再真跑。

## Destination

`DryRunHook`（order 290，HITL 前）：beforeTool 拦下入计划（结构化 block
「[干跑拦截]」——非错误标记，模型可读可改道）；计划面 `plan()`（toolCallId/
toolName/args 快照，有界 100 + 溢出计数）；include 清单空 = 全量拦；运行时
setEnabled（干跑窗口启停）。yml `buzhou.dry-run.*` 默认关。

## Notes

- 号段：spec 323 / T637–T638 / impl-346。
- 借鉴：Terraform plan/apply（先计划后执行，计划可审阅）。

## Decisions so far

- 拦截理由用「[干跑拦截]」前缀——不复用错误标记（干跑不是失败；blocked
  不走 afterTool，熔断/预算不计数）。
- 清单空 = 全量拦（纯演练语义——与混沌 include 空=全量一致）。
- 计划面有界 100 条 + dropped 计数（长演练不膨胀内存——诚实截断）。

## Out of scope

- 计划持久化/导出（export 族可后接）；plan→apply 一键放行（宿主接线）；
- 参数重写建议（Replace 面）。

## Tickets

- [x] [T637 DryRunHook + 属性 + 装配](../tickets/T637-dry-run-hook.md)（impl-346）
- [x] [T638 回归与收口](../tickets/T638-dry-run-close.md)（impl-346）
