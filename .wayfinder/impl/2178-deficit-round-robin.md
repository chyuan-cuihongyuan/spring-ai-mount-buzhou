# impl 2178 — S 会话 S28 Deficit Round Robin 亏空调度（spec 5027 / T6155–T6156 / S28）

纵切片：DeficitRoundRobin（core/policy）——亏空记账 + 轮转
服务 + 结转语义 + 容量守恒。

- 验证：`mvn -pl buzhou-core test -Dtest='DeficitRoundRobinTest'` 全绿。
