# 958 — 评估剪枝进程级兜底装配

> 来源：I 会话第 57 轮 = effort #958（impl 701）。spec 901 的装配收口（RetryBudgetHolder 进程级兜底先例同款）。

## 背景

spec 901 的 `setPrunePolicy` 只有实例级编程面——`new EvalRunner(...)` 手动构造路径（宿主普遍用法）无声明式入口。

## 目标

- `EvalPrunePolicyHolder`（进程级 AtomicReference 兜底，RetryBudgetHolder 同款）：
  - `current()/set()/clear()`；
- `EvalRunner` prune 解析：实例 `prunePolicy` 优先，否则惰性拾取 `EvalPrunePolicyHolder.current()`（装配面覆盖到非 bean 构造路径）；
- autoconfig：`buzhou.eval.prune.{enabled,min-items,fail-rate-threshold}`（enabled=true 声明即装配，DisposableBean 清理 Holder——buzhouRetryBudgetAdapter 先例）；
- 优先级：实例显式 > Holder 进程级 > 关闭。

## 兼容性

opt-in：未装配（enabled 缺省 false）时 Holder 恒 null，行为零变化。
