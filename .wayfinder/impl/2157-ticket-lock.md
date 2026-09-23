# impl 2157 — S 会话 S7 Ticket Lock 票据锁（spec 5006 / T6113–T6114 / S7）

纵切片：TicketLock（core/concurrent）——取票/候号/校验放行 +
排队深度读数 + 并发互斥对拍。

- 验证：`mvn -pl buzhou-core test -Dtest='TicketLockTest'` 全绿。
