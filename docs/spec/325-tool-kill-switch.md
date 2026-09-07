# Spec 325 — 工具紧急停用开关（effort #325）

> wayfinder map：`.wayfinder/maps/effort-325.md`（T641–T642）。借鉴：LaunchDarkly
> kill switch / K8s cordon——人工标记式隔离，不重启。

## Problem Statement

工具事故（坏版本、越权行为、下游数据污染）响应手段都是结构性的：熔断
要等错误率凑够自动跳，干跑/混沌要预先开窗——「人已判定这个工具有害，
立刻全局停掉」没有按钮。

## Solution

`ToolKillSwitchHook`（BuzhouHook，order 15 维护门后模型闸前）：

- beforeTool：停用集内工具 → block「[工具已停用]」前缀（**非错误标记**
  ——人为停用不是失败，blocked 不走 afterTool，熔断/错误预算零污染），
  文案带恢复指引；集外直通。
- 事故按钮（运行时）：`disableTools(Set)` / `enableTools(Set)` /
  `clearAll()` / `disabled()` 视图——不重启即时生效。
- 热更新：监听 `BuzhouConfigRefreshEvent`（320 通道）重读
  `buzhou.tool-kill-switch.tools` **整体覆盖**停用集（yml 是事实源——
  运行时应急改动在下一次刷新时被 yml 收编，语义诚实入档）。
- 装配恒在：空集纯直通零变化；事故按钮必须预先存在才有用（区别于
  混沌/干跑 opt-in：它们会主动做事）。

## User Stories

1. 作为值班 SRE，发现某工具吐脏数据，我想一条命令全局停它，所以事故
   面不再扩大（不用等熔断凑错误率）。
2. 作为值班 SRE，修复后我想一键恢复，所以停用不留永久伤。
3. 作为宿主，我想用配置通道（发 320 刷新事件）批量管理停用集，所以
   yml 是事实源、事件即同步。
4. 作为 SRE，人为停用不应污染熔断/错误预算统计，所以演练性停用不会
   触发误告警。

## Implementation Decisions

- 停用集 volatile 不可变副本替换（读无锁——hook 链热路径零损耗）。
- 恒装配 + BuzhouHook 自动收集（RuntimeConfig 一员）。

## Testing Decisions

- `ToolKillSwitchHookTest`：停用拦/放行/非错误标记/运行时增删清/
  disabled 视图。
- `ToolKillSwitchAssemblyTest`：恒装配；yml 预停用；刷新事件覆盖
  （addFirst PropertySource + publishEvent 外部行为路径——320 同法）。

## Out of Scope

- 会话/租户粒度；停用审计事件流；恢复审批流。

## Further Notes

- 事故响应族：熔断（自动）/ 扰乱预算（计划性）/ **紧急停用（人工即时）**
  ——三层齐。
