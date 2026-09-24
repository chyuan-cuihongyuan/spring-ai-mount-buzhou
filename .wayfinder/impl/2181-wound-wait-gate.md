# impl 2181 — S 会话 S31 Wait-Die/Wound-Wait 死锁预防时序裁决（spec 5030 / T6161–T6162 / S31）

纵切片：WoundWaitGate（core/transaction）——时间戳定年龄 +
两模式四裁决（枪伤/自裁/等待/授予）+ 年长交接 + abortedCount
读数。（勘误：原 Jump Hash 与 spec 3020 同面撞坑换静脉。）

- 验证：`mvn -pl buzhou-core test -Dtest='WoundWaitGateTest'` 全绿。
