# impl 2131 — R 会话 R31 EIP-1559 基础费调节（spec 4030 / T6061–T6062 / R31）

纵切片：Eip1559BaseFee（core/policy）——供需调节 + 弹性钳制 +
地板 + BigInteger floor 精确 + 越界 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='Eip1559BaseFeeTest'` 全绿。
