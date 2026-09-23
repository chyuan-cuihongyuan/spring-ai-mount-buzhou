# impl 2147 — R 会话 R47 fail2ban 封禁递升（spec 4046 / T6093–T6094 / R47）

纵切片：BanEscalation（core/policy）——滑窗判禁 + 前科递升 +
禁期语义 + 时钟注入 + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='BanEscalationTest'` 全绿。
