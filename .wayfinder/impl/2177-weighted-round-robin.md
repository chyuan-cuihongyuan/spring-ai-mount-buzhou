# impl 2177 — S 会话 S27 平滑加权轮询（spec 5026 / T6153–T6154 / S27）

纵切片：WeightedRoundRobin（core/policy）——平滑计重 + 最大
选择降权 + 确定性。

- 验证：`mvn -pl buzhou-core test -Dtest='WeightedRoundRobinTest'` 全绿。
