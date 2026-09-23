# impl 2158 — S 会话 S8 Seqlock 序号锁（spec 5007 / T6115–T6116 / S8）

纵切片：SeqLock（core/concurrent）——奇偶序号 + 单写者串行 +
读者乐观校验。

- 验证：`mvn -pl buzhou-core test -Dtest='SeqLockTest'` 全绿。
