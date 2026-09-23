# impl 2132 — R 会话 R32 难度目标重定（spec 4031 / T6063–T6064 / R32）

纵切片：DifficultyRetarget（core/policy）——窗式重定 + ±4×
钳制 + powLimit 封顶 + 窗滚动 + 倒流 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='DifficultyRetargetTest'` 全绿。
