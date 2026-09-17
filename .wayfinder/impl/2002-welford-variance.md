# impl 2002 — Q 会话 R2 Welford 在线方差（spec 3001 / T5003–T5004 / R2）

纵切片：WelfordAccumulator（core/metrics）——add 单遍递推 +
merge Chan 合并 + 双分母方差 + 空态/单点 NaN 诚实边界。

- 验证：`mvn -pl buzhou-core test -Dtest='WelfordAccumulatorTest'` 全绿。
