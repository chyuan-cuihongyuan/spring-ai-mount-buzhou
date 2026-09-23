# impl 2164 — S 会话 S14 Group Commit 组提交（spec 5013 / T6127–T6128 / S14）

纵切片：GroupCommitLog（core/recovery）——组内合并 + LSN 分配
+ 一次落盘 + 持久上沿。

- 验证：`mvn -pl buzhou-core test -Dtest='GroupCommitLogTest'` 全绿。
