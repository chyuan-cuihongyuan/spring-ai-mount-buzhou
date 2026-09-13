# impl 559 — BudgetRecommendation（effort #806）

## 切片

- `buzhou-core/src/main/java/.../core/budget/BudgetRecommendation.java` — 静态 recommend + nearestRank + Ring（synchronized long[] 环，head/size/dropped）。
- `buzhou-core/src/test/java/.../core/budget/BudgetRecommendationTest.java` — 6 例（环 dropped 精确账 2+1=3）。

## 口径

- 单位无关：samples 语义由调用方定义（tokens/微美元皆可）。
- 负值=null 同待遇：忽略不报错。

## 验证

mvn -pl buzhou-core -am test -Dtest='BudgetRecommendationTest' → 6/6 绿；快照再生 1 新公共类型。
