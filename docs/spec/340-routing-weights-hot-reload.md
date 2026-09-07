# Spec 340 — 路由权重热调整（effort #340）

> wayfinder map：`.wayfinder/maps/effort-340.md`（T671–T672）。C 会话第 41 轮，
> 339 的运维收尾。

## Problem Statement

多模型加权路由（339）的权重在 yml 构造期定死：金丝雀期想微调配比
（7:3 → 8:2）必须重启应用——路由面有了，调权面没有。

## Solution

- **`WeightedChatModel.setWeight(beanName, weight)`**：运行时调权——
  平滑 WRR 状态保留（current 动量不清零，比例自然收敛新配比）；
  未知 beanName 拒绝（IllegalArgumentException）。
- **`RoutingWeightsHotReload`**（ApplicationListener，320 同模式）：
  收到 `BuzhouConfigRefreshEvent` 重读 `buzhou.routing.weights` →
  对已知路逐个 setWeight；**面外名字 WARN 跳过**（新增/删除路不热加
  ——候选面构造期定死，面变更须重启；诚实边界）；`reloadCount()`
  观测面 + `buzhou.routing.reloaded` 计数。
- **装配**：路由器 bean 在场即挂监听（同条件）；无路由器零变化。

## User Stories

1. 作为运维，我想改 yml 发刷新事件就热调路由配比，所以 金丝雀期
   微调不用重启。
2. 作为运维，我想调权不清 WRR 动量，所以 切换平滑没有瞬间全量
   涌向新路。
3. 作为运维，我想 yml 里混入面外名字只 WARN 不红，所以 环境间
   yml 差异不炸刷新。
4. 作为审计者，我想热调次数有计数，所以 调权历史可查。

## Implementation Decisions

- setWeight 经名字→模型映射委派 `WeightedRouter.setWeight`（既有
  API——未知即 add 的语义在 ChatModel 装饰器层收紧为拒绝：候选面
  构造期定死）。
- 热重载绑定复用 Binder（BulkheadHotReload 同法）。

## Testing Decisions

- setWeight：3:1 调 1:3 后下一个窗口分布翻转（served 事实源断言）；
  未知名红；routes() 观测同步。
- 热重载：MutableEnvironment 改值 + 发事件 → 权重生效（320 同手法）；
  面外名字跳过不红；计数递增。
- 装配：路由器在场监听在场；未配路由无监听。

## Out of Scope

- 运行时增删候选路；权重历史审计；多实例权重同步（每实例独立
  rebind——refresh 事件广播归宿主）。

## Further Notes

- 新公共类型 `RoutingWeightsHotReload` 随轮 regenerate 快照；
  setWeight 是 339 类型的方法级增量（不增型）。
