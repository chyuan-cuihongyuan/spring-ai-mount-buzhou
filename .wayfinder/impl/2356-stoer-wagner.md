# impl 2356 — W 会话 W4 Stoer-Wagner 全局最小割（spec 9003 / W9007–W9008 / W4）

纵切片：Stoer-Wagner 全局最小割——StoerWagnerMinCut（core/concurrent）：MAO 阶段制无向全局最小割 O(V³)。

- 验证：`mvn -pl buzhou-core test -Dtest='StoerWagnerMinCutTest'` 全绿。
