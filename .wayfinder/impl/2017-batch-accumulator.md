# impl 2017 — Q 会话 R17 批量攒批器（spec 3016 / T5033–T5034 / R17）

纵切片：BatchAccumulator（core/concurrent）——双阈值攒批 + 守恒
对账 + 确定性时间传入。

- 验证：`mvn -pl buzhou-core test -Dtest='BatchAccumulatorTest'` 全绿。
