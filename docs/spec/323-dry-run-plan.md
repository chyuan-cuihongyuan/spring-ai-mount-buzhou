# Spec 323 — 干跑拦截与执行计划（effort #323）

> wayfinder map：`.wayfinder/maps/effort-323.md`（T637–T638）。借鉴：Terraform
> plan/apply——先计划后执行，计划可审阅。

## Problem Statement

高危 agent 的"将会做什么"只有真执行后才可见：HITL 逐个批太碎，直接跑
又不敢——缺一个「先看全量计划再放行」的中间档。

## Solution

`DryRunHook`（BuzhouHook，order 290 HITL 前）：

- beforeTool：干跑开时拦下入计划——HookResult.block 带「[干跑拦截]」
  结构化前缀（非错误标记：干跑不是失败；blocked 不走 afterTool，熔断/
  错误预算不被污染），模型可读可改道继续对话。
- 计划面 `plan()`：不可变 `PlannedCall(toolCallId, toolName, arguments)`
  列表（args 快照）；有界 100 条，超出 dropped 计数（诚实截断）。
- include 清单（tools）空 = 全量拦（纯演练）；清单内 = 只拦清单工具。
- 运行时 `setEnabled`（干跑窗口启停不重启）。
- yml `buzhou.dry-run.{enabled=false, tools}`——默认关。

## User Stories

1. 作为运维，我想对高危 agent 开干跑窗口跑一轮任务，所以拿到「将要调用
   哪些工具、什么参数」的全量计划单。
2. 作为运维，计划过目后我想停干跑重跑真执行，所以窗口切换不重启。
3. 作为开发者，我只想对写类工具开干跑（清单），所以读类工具照常真跑。
4. 作为 SRE，干跑拦截不应污染熔断/错误预算统计，所以演练不引发误跳闸。

## Implementation Decisions

- 拦截文案「[干跑拦截]」独立前缀——不进 ToolFeedbackType 错误两档。
- 计划条目记 args 快照（Map.copyOf——后续参数变更不影响已记计划）。
- afterTool 恒 CONTINUE（干跑只拦执行不篡改结果）。

## Testing Decisions

- `DryRunHookTest`：拦入计划（id/名/args 快照）/全量 vs 清单/停跑放行/
  运行时开关/有界 100 + dropped/afterTool 不动/计数。
- `DryRunAssemblyTest`：enabled 装配；默认不装；tools 绑定。

## Out of Scope

- 计划持久化导出（export 族可后接）；plan→apply 一键放行；参数重写建议。

## Further Notes

- 演练族齐：混沌（322 韧性演练）/ **干跑（323 行为演练）**——一个是
  "乱打验证扛不扛得住"，一个是"先看清楚要做什么"。
