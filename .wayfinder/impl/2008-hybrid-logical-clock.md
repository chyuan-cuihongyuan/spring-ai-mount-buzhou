# impl 2008 — Q 会话 R8 混合逻辑时钟（spec 3007 / T5015–T5016 / R8）

纵切片：HybridLogicalClock（core/concurrent）——Hlc(wall,counter)
+ tick 严格单调 + observe 三路合并 + 物理钟注入。

- 验证：`mvn -pl buzhou-core test -Dtest='HybridLogicalClockTest'` 全绿。
