# impl 2135 — R 会话 R35 JSON Patch 应用（spec 4034 / T6069–T6070 / R35）

纵切片：JsonPatchApplier（core/policy）——六操作 + 指针转义 +
原子应用 + move 下标记账。

- 验证：`mvn -pl buzhou-core test -Dtest='JsonPatchApplierTest'` 全绿。
