# impl 2133 — R 会话 R33 祖先费率打包（spec 4032 / T6065–T6066 / R33）

纵切片：AncestorFeerate（core/policy）——祖先闭包聚合费率 +
去重 + 拓扑闭包 + 贪心优先打包序 + 环 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='AncestorFeerateTest'` 全绿。
