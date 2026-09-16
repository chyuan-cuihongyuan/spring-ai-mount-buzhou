# impl 1584 — 维护触发器（spec 2033 / T3167–T3168 / R34）

纵切片：`MaintenanceTrigger`（core/recovery 主）+
`MaintenanceTriggerTest`（七用例）。比例阈值、全死必清、间隔兜底、
触发记账。

- 测试：`mvn -pl buzhou-core test -Dtest=MaintenanceTriggerTest` 7/7 绿。
