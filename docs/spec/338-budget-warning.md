# Spec 338 — 预算软预警线（effort #338）

> wayfinder map：`.wayfinder338/MAP.md`（T667–T668）。C 会话第 39 轮。

## Problem Statement

预算闸只有硬顶：撞墙才知道。撞墙前的 80% 区间零信号——使用者没有
机会在限额前加额度、收束对话或切换 key，只能吃 hard-stop 终止。

## Solution

AWS Budgets 思想——percent-of-budget 预警：

- **`buzhou.token-budget.warning-percent`**（默认 80；-1 关闭）。
- **三维判定**：会话总 token、会话成本（USD）、虚拟 key token——
  消耗达对应硬顶的该百分比即发 `budget.warning` 事件
  （payload：dimension/limit/value/warningPercent）+ 计数
  `buzhou.budget.warnings`（tag dimension）。
- **一次一发**：消耗单调递增，warned 即终局（同维度不再重复）；
  会话总量与成本两个维度各发各的；已耗尽 key 走 hard-stop 语义
  不再预警。
- **仅事件不拦截**：控制流与既有硬顶闸完全一致（零行为变化）。
- 判定整数化（value×100 ≥ limit×percent）——无浮点。

## User Stories

1. 作为使用者，我想消耗到 80% 就收到预警事件，所以 撞 hard-stop 前
   有机会加额度或收束。
2. 作为多租户运营，我想虚拟 key 接近限额时预警，所以 充值/换 key
   不必等到业务被 hard-stop 打断。
3. 作为运维，我想默认不用配置就有 80% 预警（仅事件零拦截），所以
   升级即得、零风险。
4. 作为审计者，我想预警有计数可查，所以 预警面本身可观测。
5. 作为使用者，我想 -1 一键关闭软预警，所以 噪声敏感场景可退。

## Implementation Decisions

- 预警标记内存面（per session×dimension / per key×dimension），1024
  上限诚实降级；重启清零——重启后若仍在预警区会再发一次（可接受：
  预警优于沉默）。
- 属性类加 warningPercent（5 参兼容构造保留——既有调用方零破坏）。

## Testing Decisions

- 交叉 80% 发一次（继续消耗不再发）/ 未达不发 / -1 关闭 /
  成本维度 / key 维度（TokenBudgetHookEndToEndTest 同装配手法）。

## Out of Scope

- 预警回调/通知路由（事件面归 312/330 告警族消费）；多级阶梯线；
- 跨窗口重置联动（reset 后 warned 清零随 VirtualKeys.reset 同步——
  记档不做：重启语义已覆盖）。

## Further Notes

- 属性扩展无新公共类型——快照无增量（regenerate 确认零 diff 即可）。
