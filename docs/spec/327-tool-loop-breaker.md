# Spec 327 — 工具循环断路器（effort #327）

> wayfinder map：`.wayfinder327/MAP.md`（T645–T646）。借鉴：326 重复检测
> 的工具面平移——LLM agent 最烧钱的失控形态。

## Problem Statement

模型陷入"同工具同参数"循环调用时：每次调用都真执行（下游配额/费用/
副作用照发生），而参数未变结果不会变——只能等 token 预算或模型调用上限
兜底砍整个轮次，损失已发生且无中途干预点。

## Solution

`ToolLoopBreakerHook`（BuzhouHook，order 245 熔断后——熔断先按错误率跳闸，
本闸按"调用形态"断路）：

- beforeTool：key = toolName + args Map.hashCode（与顺序无关——稳定）；
  相邻同 key 累计 run；不同 key 重计（解闩）。run 达窗（默认 3）→
  block「[工具循环]」+ 干预指令（换参数/换工具/以当前结果收束）+ 计数器
  `buzhou.runaway.tool-loop.broken`。
- 默认干预（区别于 326 observe 默认）：装配即干预——工具调用是真执行，
  复读文本只占上下文，循环工具在烧真配额。
- per-session 跟踪；超 1024 会话整体重置（326 同款诚实降级）。
- yml `buzhou.runaway.tool-loop.window`（未配不装配零变化）。

## User Stories

1. 作为运维，agent 连续 3 次同参调用同一工具时我想被拦下并回填换参
   指令，所以下游配额不再被无效循环烧。
2. 作为模型，被拦时我想知道"为什么拦、接下来怎么办"，所以 block 文案
   带三选一出路（换参/换工具/收束）。
3. 作为运维，换了参数的调用是新尝试不该拦，所以 key 含参数——换参即解闩。

## Implementation Decisions

- args 取 Map.hashCode（顺序无关）；null args 按空表。
- 交替循环（a,b,a,b）不在本闸——只抓同 key 连续，序列模式检测诚实
  另立项。

## Testing Decisions

- `ToolLoopBreakerHookTest`：达窗拦+文案/换参解闩/换工具重计/闩住不重复
  拦同 key？——每条同 key 超窗都拦（持续干预直到换 key——工具面与文本面
  不同：不拦就继续烧）/会话隔离/null 防御。
- 装配：window 配置即 bean；未配不装；window=1 红。

## Out of Scope

- 交替/周期序列检测；语义近似参数判同；自动改参重试。

## Further Notes

- 打转治理齐：文本面（326 观测+opt-in 解困）/ **工具面（327 默认断路）**。
