# Spec 322 — 工具混沌注入（effort #322）

> wayfinder map：`.wayfinder322/MAP.md`（T635–T636）。借鉴：Netflix
> Chaos Monkey——平时按概率注入故障，演练韧性机制。

## Problem Statement

熔断/重试预算/舱/错误预算等韧性机制只能等真实故障才动一次：它们是否
真的按设计联动，平时无验证手段——首次实战可能就是配置错误被发现的那天。

## Solution

`ChaosMonkeyHook`（BuzhouHook，order 235 先于熔断 240）：

- beforeTool 按概率注入两种袭击：**延迟**（sleep 固定毫秒——模拟慢工具，
  让舱 acquire-timeout/泳道超时被真实验证）；**故障**（HookResult.block
  结构化错误标记——131/321 同语义，模型可读可改道，重试/熔断真实触发）。
- 概率源可注入（DoubleSupplier [0,1)——测试确定性序列）；percent ∈ [0,100]。
- include 工具清单（空 = 全量）；运行时开关 setEnabled（演练窗口启停）；
  计数器 latency/fault 注入次数。
- yml `buzhou.chaos.{enabled=false, latency-percent, latency-millis,
  exception-percent, tools}`——默认关，配置 enabled=true 装配。

## User Stories

1. 作为 SRE，我想在演练窗口给 10% 的工具调用注入 500ms 延迟，所以舱
   超时/泳道排队的行为被真实验证而不是纸面推断。
2. 作为 SRE，我想按 5% 注入工具故障，所以熔断跳闸/重试预算扣减/错误
   预算燃尽在平时就真实走一遍。
3. 作为 SRE，我想限定混沌只打指定工具（tools 清单），所以演练不误伤
   关键路径。
4. 作为 SRE，我想运行时启停（不重启），所以演练窗口开始结束随手翻。

## Implementation Decisions

- 故障走 block 不走异常：hook 契约内表达，与熔断拒同词汇族。
- 延迟为同步 sleep（hook 链同步模型内诚实）。
- 一次 beforeTool 至多一种袭击（先判延迟再判故障，同概率独立掷）。
- afterTool 不动（混沌不篡改真实结果——结果篡改另立项）。

## Testing Decisions

- `ChaosMonkeyHookTest`：确定性概率源（序列注入）——延迟注入耗时可测/
  故障 block 标记可判/概率零不袭/include 外不袭/运行时开关翻生效/计数。
- `ChaosAssemblyTest`：enabled=true 装配 hook bean；默认不装；percent
  越界启动红。

## Out of Scope

- 结果篡改（replaceResult）；进程资源混沌（CPU/内存/磁盘）；定时调度
  全局袭击（宿主接——hook 即缝）。

## Further Notes

- 韧性自验证闭环：混沌（本轮）→ 熔断 131 / 重试预算 302 / 错误预算 321
  / 舱 84 全部可被主动演练。
