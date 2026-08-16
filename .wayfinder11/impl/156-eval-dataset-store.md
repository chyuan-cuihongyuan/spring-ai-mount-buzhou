# 156 — 评估数据集实体与合成会话 store

**Parent:** spec 52 §A / [T190](../tickets/T190-eval-dataset-store.md)

**Status:** done

- [x] EvalDatasetStore（core.eval 包）：合成会话 `__buzhou.eval__`（fsck 天然豁免口径入档）；
  键布局 `eval.ds.<name>` / `eval.ds.<name>.item.<000001>`（键序即添加序；scanByPrefix 下推复用）
- [x] EvalDatasetMeta / EvalItem（sourceSessionId+sourceTurnSeq 溯源 = Langfuse sourceTraceId 收窄）
- [x] create/list/addItem/items/delete；新 ErrorCode `EVAL_OPERATION_INVALID`（非法名/重名/未建集/
  空字段 fail-fast）
- [x] 实现期纠偏：deleteDataset 前缀串删防护（"reg" 不得误删 "reg-2"——子键过滤 `<name>.` 后代）
- [x] 3 测试绿（生命周期/四拒绝/相邻前缀边界）
