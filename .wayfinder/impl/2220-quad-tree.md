# impl 2220 — T 会话 T20 QuadTree 四叉树（spec 6019 / T6239–T6240 / T20）

纵切片：QuadTree（core/policy）——桶容量四分+矩形相交剪枝
（源码随 T18 对账批预入档；象限判定 `>midX` 与子界
`[midX+1,…]` 对齐根治边界点漂移）。

- 验证：`mvn -pl buzhou-core test -Dtest='QuadTreeTest'` 全绿；随 T18 verify 三门绿。
