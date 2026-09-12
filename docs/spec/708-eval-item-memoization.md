# 708 — 评估项结果记忆化

> 来源：G 会话第 9 轮 = effort #708（EvalRunner 缓存深化）/ [T1016](../../.wayfinder/tickets/T1016-eval-memoization.md) / [T1017](../../.wayfinder/tickets/T1017-eval-memoization-verify.md) / impl 608。

## Problem

EvalRunner 每次回归跑都对全数据集重烧模型调用——judge 逻辑改一行、数据集没动的项也要全部重跑。评估成本（token+时延）随迭代次数线性放大，而其中大半是「数据集与判定逻辑都没变」的重复计算。工具层有 MemoizedToolCallback、run 层有预算闸，项粒度的结果复用不存在。

## Solution

scikit-learn Pipeline `memory` 思想（≈65K star：无变化步骤直接复用拟合结果）：

- **opt-in 激活**：`setMemoizationKey(String key)`——key 是判定身份指纹（judge 实现+模型+模板版本由调用方拼装）；null/blank = 关（默认零行为）。
- **签名**：`sig = sha256(datasetName | itemId | input | expected | key)`——数据集就地改项（input/expected 变）自动失配（82 指纹同域语义下放到项粒度）。
- **存储**：stateStore 合成会话（与 run 记录同域不同前缀段）`eval.memo.<dataset>.<itemId>` → JSON `{sig,status,detail,actual,durationMs}`。
- **命中**：sig 一致 → 跳过模型调用与 judge，直接复用上轮三态结果——detail 加 `[MEMO] ` 前缀（[RETRIED] 同风格留痕）+ `buzhou.eval.memo.hits` 计数；miss/失配 → 照常执行后回写 + `buzhou.eval.memo.misses`。
- **不缓存 ERROR**：执行异常/评估器异常是瞬时的，缓存会固化故障。
- **降级**：memo 读写抛错只 WARN 不 fail run（优化不是依赖）。

## User Stories

1. judge 迭代：改判定阈值后重跑——未变项全部 [MEMO] 命中零 token，只有判定结果随 judge 语义变化的项真实重算（sig 含 key，key 不变命中、key 变全量重跑——调用方自控）。
2. CI 评估：PR 只改 prompt 模板——换 memoizationKey 分两跑对比；数据集与 judge 均未变的例行跑全部命中。

## Implementation Decisions

- memo 化包装在 `runItemWithRetry` 外层（预算记账内层）——预算先于缓存记账（预算耗尽项不查缓存不回写，口径一致）。
- 写入 producer="eval"（与 run 记录同域）；ttlTurns=null（随合成会话生命周期）。
- sig 用 MessageDigest sha256 hex（core crypto 族同口径，无新依赖）。

## Testing Decisions

- 命中：同 key 二跑——第二次模型调用数为 0、hits=1、detail 带 [MEMO]、status/actual 与首轮一致。
- 失配：改 expected → sig 失配重跑、misses 计数、memo 覆写。
- 默认关：不设 key 二跑模型调用数 ×2（零行为）。
- ERROR 不缓存：首轮执行异常项二跑真实重跑。

## Out of Scope

- 模型漂移捕捉（缓存语义刻意排除——归漂移基线/评估对比族）。
- 跨实例共享 memo（stateStore 已提供何种共享语义就继承何种——不新增）。
- judge 带随机性场景的一致性（调用方换 key 失效——文档明示）。

## Further Notes

与 520（预算：成本上限）、535（重试：抖动缓解）、609（超时：挂死收敛）同列——EvalRunner 执行策略四件套补最后一块「成本复用」。
