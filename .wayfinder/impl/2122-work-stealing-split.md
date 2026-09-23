# impl 2122 — R 会话 R22 工作窃取对半分割（spec 4021 / T6043–T6044 / R22）

纵切片：WorkStealingSplit（core/concurrent）——对半定量 + 冷端搬运。

- 验证：`mvn -pl buzhou-core test -Dtest='WorkStealingSplitTest'` 全绿。
