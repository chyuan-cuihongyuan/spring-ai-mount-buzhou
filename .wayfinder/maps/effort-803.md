# effort #803 — 检索多路改写融合

- 会话：H 会话 800 系第 4 轮 ｜ spec [803](../../../docs/spec/803-multi-query-retriever.md) ｜ 票 [T1107](../tickets/T1107-multi-query-retriever.md)/[T1108](../tickets/T1108-multi-query-retriever-verify.md) ｜ impl556
- 借鉴：LangChain MultiQueryRetriever（langchain-ai/langchain ≈105K star）——查询多角度改写后并检索再融合

## 勘察（排重）

- HybridSkillRanker（605/744）：RRF 但融合<b>单查询双信号</b>（文本×向量）。
- RecallSearch HYBRID：同上族——查询改写维度缺位。
- grep -i `MultiQuery|QueryRewrite|rewrite`：无命中。RRF 常数 k=60 与 605 同款（口径一致）。

## 决定

`MultiQueryRetriever`（memory.recall）：variants 生成器（函数注入——测试确定性/生产 LLM 或规则）展开原查询为 N 变体，每变体独立过 base 检索得各排名，跨变体 RRF 融合（k=60）；去重键=消息 id（多变体命中分数累加是奖励）；score 字段承载 RRF 值、mode=multi-rrf 留痕。变体封顶 8（超限截断）；生成器故障/空变体回退单路原查询、单路检索故障隔离跳过（fail-open 双层）。Result 带 variantsExecuted/uniqueHits 口径。

## 测试

跨变体登顶+RRF 精确值(1/61+1/62)/同消息三变体去重累加 3/61/单路抛异常隔离/生成器故障回退原查询/空与 null 变体回退/变体封顶 8/空查询+limit 边界/limit 截断/fail-fast——9 例全绿。

## 诚实边界

变体生成质量归生成器（本类只做融合判定——不做同义词典）；融合分数是 RRF 值非相关度（跨变体可比、跨检索器不可比）；fail-open 语义声明（生成器挂≠检索挂）；变体内名次=列表序（base 契约）。
