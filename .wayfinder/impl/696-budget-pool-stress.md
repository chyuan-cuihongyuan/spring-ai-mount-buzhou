# 696 — ElasticBudgetPool 并发守恒压测（第 47 轮切片补写）

**What to build:** 8 线程×500 借还 Σheld+surplus==capacity 守恒 + base 保底不吃 borrow。

**Status:** done

- [x] ElasticBudgetPoolStressTest 两用例全绿（对账轮补写切片）
