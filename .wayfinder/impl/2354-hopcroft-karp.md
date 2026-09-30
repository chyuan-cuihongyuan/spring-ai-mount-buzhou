# impl 2354 — W 会话 W2 Hopcroft-Karp 二分图最大匹配（spec 9001 / W9003–W9004 / W2）

纵切片：Hopcroft-Karp 二分图最大匹配——HopcroftKarpMatcher（core/concurrent）：分层 BFS+当前弧 DFS 阶段制 O(E√V)。

- 验证：`mvn -pl buzhou-core test -Dtest='HopcroftKarpMatcherTest'` 全绿。
