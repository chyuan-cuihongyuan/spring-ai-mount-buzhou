# impl 2259 — U 会话 U8 FloydWarshall 全对最短路（spec 7007 / U7215–U7216）

纵切片：见 spec；复用同包图族先例（Dijkstra/DisjointSet/Tarjan）。

- 验证：`mvn -pl buzhou-core test -Dtest='FloydWarshallTest'` 全绿。
