# impl 2104 — R 会话 R4 Boyer-Moore 多数表决（spec 4003 / T6007–T6008 / R4）

纵切片：BoyerMooreMajority（core/metrics，纯静态）——配对抵消 +
二次核验 + 恰半僵局语义。

- 验证：`mvn -pl buzhou-core test -Dtest='BoyerMooreMajorityTest'` 全绿。
