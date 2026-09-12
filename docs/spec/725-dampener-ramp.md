# 725 — 健康压权半开中点渐变

> 来源：G 会话第 26 轮 = effort #725（703 dampener 深化；HAProxy slow-start 思想）/ [T1050](../../.wayfinder/tickets/T1050-dampener-ramp.md) / [T1051](../../.wayfinder/tickets/T1051-dampener-ramp-verify.md) / impl 625。

## Problem

703 的恢复是一步到位：CLOSED 瞬间权重从地板跳回声明值——半开探测刚成功的 provider（往往仍在恢复期）立刻承接满量流量，二次跳闸风险高。HAProxy slow-start 惯例：恢复 server 权重爬升而非跳变。

## Solution

确定性两级阶梯（无需调度线程）：

- **OPEN**：权重= floor（不变）；
- **HALF_OPEN**：权重= (floor+declared)/2 向下取整——半开探测期即承载半量试探流量；
- **CLOSED**：权重= declared（不变）；
- dampened() 读数扩展：行值反映当前档（half 档也在 dampened 视图内，语义=「未回满声明」）。

## Implementation Decisions

- half 档仍在 dampened() 视图（语义=未回满）——消费端不必理解三态。
- 无时间窗（秒级爬升需调度线程——两确定性档已覆盖主风险，诚实边界）。

## Testing Decisions

真状态机三段：OPEN→floor；时钟推进→HALF_OPEN→中点；探测成功→CLOSED→声明值。奇数中点向下取整断言。

## Out of Scope

时间窗 slow-start（调度线程）。
