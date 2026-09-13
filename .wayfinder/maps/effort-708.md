# effort #708 — 评估项结果记忆化（换题注记：原池「评估项结果记忆化」保持）

- 会话：G 会话 700 系第 9 轮 ｜ spec [708](../../../docs/spec/708-eval-item-memoization.md) ｜ 票 [T1016](../tickets/T1016-eval-memoization.md)/[T1017](../tickets/T1017-eval-memoization-verify.md) ｜ impl608
- 借鉴：scikit-learn（≈65K star）Pipeline memory 参数——拟合步骤记忆化；LangSmith run 复用同思想

## 勘察（排重）

- EvalRunner 全链成熟（预算 520/重试 535/超时 609/期望 198/指纹 82）——**无项级记忆化**：回归跑 judge 改一行，全部模型调用重烧一遍。
- MemoizedToolCallback/TurnMemoHook 是工具层缓存——评估层无。
- datasetFingerprint（82）只入档不用于缓存键。

## 决定

opt-in：`setMemoizationKey(key)`（null=关默认）激活——sig=sha256(dataset|itemId|input|expected|key)，stateStore 落 `eval.memo.<dataset>.<itemId>`（{sig,status,detail,actual,durationMs} JSON）；命中跳过模型调用复用上轮判定（detail 加 `[MEMO] ` 前缀同 [RETRIED] 风格），sig 失配或 miss 重跑并回写；ERROR 项不缓存（瞬时异常不可复用）；读写失败降级直跑（memoization 是优化不是依赖）；计数 buzhou.eval.memo.hits/misses。

## 测试

命中零模型调用+`[MEMO]` 前缀/失配重跑回写/默认关零行为+ERROR 不缓存。

## 诚实边界

模型自身漂移不由本面捕捉（缓存口径=「数据+judge 未变→上轮判定仍有效」——模型漂移归漂移基线族）；判定语义不可复用场景（judge 带随机）由调用方换 key 失效。
