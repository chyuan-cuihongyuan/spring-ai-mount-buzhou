# impl 2113 — R 会话 R13 区块 min/max 剪枝（spec 4012 / T6025–T6026 / R13）

纵切片：ZoneMapPruner（core/cleanup）——区块统计 + 含等重叠裁决 +
剪枝账。

- 验证：`mvn -pl buzhou-core test -Dtest='ZoneMapPrunerTest'` 全绿。
