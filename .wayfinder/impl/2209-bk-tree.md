# impl 2209 — T 会话 T9 BK 树（spec 6008 / T6217–T6218 / T9）

纵切片：BkTree（core/metrics）——距离分叉+三角不等式剪枝
邻域索引 + 动态 add 幂等 + 暴力圣像（集+字典序）。

- 验证：`mvn -pl buzhou-core test -Dtest='BkTreeTest'` 全绿（MVN_EXIT=0）。
