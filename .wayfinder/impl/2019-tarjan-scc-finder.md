# impl 2019 — Q 会话 R19 Tarjan 强连通分量（spec 3018 / T5037–T5038 / R19）

纵切片：TarjanSccFinder（core/concurrent）——迭代 Tarjan + 反拓扑
序组件 + 环成员指认。

- 验证：`mvn -pl buzhou-core test -Dtest='TarjanSccFinderTest'` 全绿。
