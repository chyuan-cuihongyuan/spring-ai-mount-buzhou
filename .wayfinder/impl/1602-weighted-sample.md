# impl 1602 — 加权无放回抽样（spec 2051 / T3203–T3204 / R52）

纵切片：`WeightedSample`（core/eval 主）+ `WeightedSampleTest`（七用例）。
key=u^(1/w) 无放回、零权除外、回放。

- 测试：`mvn -pl buzhou-core test -Dtest=WeightedSampleTest` 7/7 绿。
- 教训入档：局部泛型 record 方法引用要显式类型 lambda；lambda 参数
  不得遮蔽方法参数名。
