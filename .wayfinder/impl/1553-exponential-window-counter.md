# impl 1553 — 指数直方图滑窗计数（spec 2002 / T3105–T3106 / R3）

纵切片：`ExponentialWindowCounter`（core/metrics 主）+
`ExponentialWindowCounterTest`（七用例）。(capacity, first, last) 三元
组桶、同容量 ≤2 合并翻倍 set 回原位、过期惰性清出、全界内计全/跨界
计半、errorBound 自描述、bucketCount 空间账。

- 测试：`mvn -pl buzhou-core test -Dtest=ExponentialWindowCounterTest` 7/7 绿。
- 教训入档：首版 addLast 合并破坏桶序（4 红）——合并桶必须原位 set；
  桶需双端口径（first/last）才能精确判跨界。
