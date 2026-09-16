# impl 1581 — 重定向预算（spec 2030 / T3161–T3162 / R31）

纵切片：`RedirectBudget`（buzhou-tools http 主）+ `RedirectBudgetTest`
（七用例）。跳数预算、访问集环检测、锚定语义。

- 测试：`mvn -pl buzhou-tools test -Dtest=RedirectBudgetTest` 7/7 绿。
