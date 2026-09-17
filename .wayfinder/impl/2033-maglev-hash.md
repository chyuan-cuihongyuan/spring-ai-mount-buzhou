# impl 2033 — Q 会话 R33 Maglev 哈希（spec 3032 / T5065–T5066 / R33）

纵切片：MaglevHash（core/policy）——步进置换抢占填表 + 查表 O(1) +
份额对账面。

- 验证：`mvn -pl buzhou-core test -Dtest='MaglevHashTest'` 全绿。
