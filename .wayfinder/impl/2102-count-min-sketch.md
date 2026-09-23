# impl 2102 — R 会话 R2 Count-Min 素材计数（spec 4001 / T6003–T6004 / R2）

纵切片：CountMinSketch（core/metrics）——d×w 矩阵 + 行最小估计 +
单侧误差 + totalCount 守恒。

- 验证：`mvn -pl buzhou-core test -Dtest='CountMinSketchTest'` 全绿。
