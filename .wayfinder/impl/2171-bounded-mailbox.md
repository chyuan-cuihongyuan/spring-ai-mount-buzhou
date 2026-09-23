# impl 2171 — S 会话 S21 Bounded Mailbox 有界信箱（spec 5020 / T6141–T6142 / S21）

纵切片：BoundedMailbox（core/backpressure）——容量信箱 + 两种
溢出策略 + 丢弃计数。

- 验证：`mvn -pl buzhou-core test -Dtest='BoundedMailboxTest'` 全绿。
