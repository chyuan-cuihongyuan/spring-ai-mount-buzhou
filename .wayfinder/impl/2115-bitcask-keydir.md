# impl 2115 — R 会话 R15 Bitcask 键目录合并（spec 4014 / T6029–T6030 / R15）

纵切片：BitcaskKeydir（core/cleanup）——追加覆盖 + 死账 + 死比门 +
压实执行。

- 验证：`mvn -pl buzhou-core test -Dtest='BitcaskKeydirTest'` 全绿。
