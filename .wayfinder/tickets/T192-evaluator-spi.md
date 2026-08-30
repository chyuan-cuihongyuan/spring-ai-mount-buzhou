---
Type: task
Status: closed
---
## Question

Evaluator SPI（evaluate(actual, expected, item) → EvalScore{passed, detail}）+
内置三评估器：EXACT / CONTAINS / JSON_PATH（jsonpath 走正则轻实现或现有依赖盘点）；
LLM-as-judge 留 SPI 口不做硬门（边界入档）。

## Resolution

impl-158 落地：SPI + EvalScore（detail 512 截断）；内置 EXACT/CONTAINS/REGEX——JSON_PATH
裁定不做（jayway json-path 不在依赖树，零新依赖纪律；票面「正则轻实现」修订为独立 REGEX
评估器，find 语义）；非法正则构造期 fail-fast；LLM-as-judge SPI 即口不内置。5 测试绿。
T192 关闭。
