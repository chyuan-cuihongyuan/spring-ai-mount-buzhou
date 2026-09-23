# impl 2121 — R 会话 R21 tokio 协作预算（spec 4020 / T6041–T6042 / R21）

纵切片：CoopBudget（core/concurrent）——N 点预算 + charge 扣减 +
yield 重置 + 尽后再扣 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='CoopBudgetTest'` 全绿。
