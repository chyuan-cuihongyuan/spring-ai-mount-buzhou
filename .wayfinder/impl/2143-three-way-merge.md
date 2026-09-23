# impl 2143 — R 会话 R43 三方合并（spec 4042 / T6085–T6086 / R43）

纵切片：ThreeWayMerge（core/policy）——LCS diff + hunk 对账 +
同改归一 + 冲突标记物化。

- 验证：`mvn -pl buzhou-core test -Dtest='ThreeWayMergeTest'` 全绿。
