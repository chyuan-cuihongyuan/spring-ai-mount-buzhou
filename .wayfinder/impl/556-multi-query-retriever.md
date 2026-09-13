# impl 556 — MultiQueryRetriever（effort #803）

## 切片

- `buzhou-memory/src/main/java/.../memory/recall/MultiQueryRetriever.java` — variants Function + base BiFunction 注入；变体清洗（null/空白滤除）+封顶 8；逐路 base.apply 故障隔离；RRF 累加（LinkedHashMap 保首见稳定序）+消息 id 去重；降序取 limit；Hit.score=RRF、mode=multi-rrf；Result(variantsExecuted/uniqueHits)。
- `buzhou-memory/src/test/java/.../memory/recall/MultiQueryRetrieverTest.java` — 9 例（BuzhouMessage 12 参构造对齐既有测试 helper 形状）。

## 口径

- 同分稳定序=首见序（LinkedHashMap 迭代序）——测试锁定。
- uniqueHits=融合前去重消息数（非最终 limit 后条数）。
- 变体是否含原查询由生成器决定（回退路径才强制原查询）。

## 验证

mvn -pl buzhou-memory -am test -Dtest='MultiQueryRetrieverTest' → 9/9 绿；快照再生 1 新公共类型。
