# impl 2250 — T 会话 T49 Dijkstra 最短路（spec 6049 / T6297–T6298 / T49）

纵切片：DijkstraShortestPath（core/concurrent）——贪心已决
集扩张 + 同包 IndexedHeap 减键松弛。

- 验证：`mvn -pl buzhou-core test -Dtest='DijkstraShortestPathTest'` 五测全绿。
