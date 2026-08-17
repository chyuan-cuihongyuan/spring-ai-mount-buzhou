---
Type: task
Status: closed
---
## Question

EvalDatasetStore：合成会话 `__buzhou.eval__`（对齐 outbox 先例；fsck 天然豁免口径入档）；
dataset/item 键前缀设计（scanByPrefix 下推复用）；create/list/addItem/items/delete；
item 含 input/expected/sourceSessionId/sourceTurnSeq 溯源（Langfuse sourceTraceId 语义收窄）。

## Resolution

impl-156 落地：合成会话 + 双前缀键布局（条目 6 位零填充键序即添加序）；治理面五操作全绿；
溯源字段往返保真；新码 EVAL_OPERATION_INVALID 挂四类 fail-fast；实现期纠偏 deleteDataset
前缀串删（相邻数据集名防护 + 回归断言）。T190 关闭。
