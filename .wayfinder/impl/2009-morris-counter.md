# impl 2009 — Q 会话 R9 Morris 近似计数器（spec 3008 / T5017–T5018 / R9）

纵切片：MorrisCounter（core/metrics）——概率增量 + 2^v−1 无偏估计 +
指数封顶 + 回放注入。

- 验证：`mvn -pl buzhou-core test -Dtest='MorrisCounterTest'` 全绿。
