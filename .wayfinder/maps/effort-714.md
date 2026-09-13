# effort #714 — 相似度阈值判定器

- 会话：G 会话 700 系第 15 轮 ｜ spec [714](../../../docs/spec/714-similarity-evaluator.md) ｜ 票 [T1028](../tickets/T1028-similarity-evaluator.md)/[T1029](../tickets/T1029-similarity-evaluator-verify.md) ｜ impl614
- 借鉴：HELM（≈10K star）grading scales 思想——判定从二值扩展到分数带；trigram Jaccard 为标准廉价文本相似度（IR 惯例）

## 勘察（排重）

- BuiltInEvaluators 仅 EXACT/CONTAINS/regex 三判——LLM 输出词序微变即误判 fail（脆判）；无相似度/分数族。
- EvalScore 仅 passed+detail（无 score 字段——分数口径先落 detail，后续分布分析可解析）。
- grep similarity/Jaccard：零命中。

## 决定

`BuiltInEvaluators.similarity(minRatio)`：字符 trigram 集合 Jaccard ≥ 阈值 → pass；detail 携带 `similarity=0.xxxxxx`（分数留痕——后续漂移基线可解析）；minRatio ∈ [0,1] fail-fast（边界含等号）。语言无关（CJK/拉丁同口径）；空串对=0（双方全空=1——退化一致）。判定函数纯静态。

## 测试

全等 1.0 过/无关 0 不过/阈值边界含等号/大小写不敏感/minRatio 越界拒绝/空串口径。

## 诚实边界

trigram Jaccard 是廉价相似度（非语义相似度——语义归嵌入/LLM-judge 族，边界沿用 #7）；分数在 detail 非独立字段（run 记录 schema 不动）。
