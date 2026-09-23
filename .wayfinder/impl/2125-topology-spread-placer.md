# impl 2125 — R 会话 R25 拓扑约束放置（spec 4024 / T6049–T6050 / R25）

纵切片：TopologySpreadPlacer（core/policy）——maxSkew 放置后斜度
裁决 + 先填最空确定性序。

- 验证：`mvn -pl buzhou-core test -Dtest='TopologySpreadPlacerTest'` 全绿。
