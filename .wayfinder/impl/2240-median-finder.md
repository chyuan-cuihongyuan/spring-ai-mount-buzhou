# impl 2240 — T 会话 T40 Median Finder 双堆中位数流（spec 6040 / T6279–T6280 / T40）

纵切片：MedianFinder（core/concurrent）——双堆夹逼+再平衡
（源码本轮入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='MedianFinderTest'` 全绿（MVN_EXIT=0）。
