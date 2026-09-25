# impl 2208 — T 会话 T8 Myers O(ND) Diff（spec 6007 / T6215–T6216 / T8）

纵切片：MyersDiff（core/metrics）——贪心 O(ND) V 数组+轨迹
回溯三态脚本 + DP oracle 最优性 + 双空早退守卫。

- 验证：`mvn -pl buzhou-core test -Dtest='MyersDiffTest'` 全绿（MVN_EXIT=0）。
