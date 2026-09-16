# impl 1613 — 回绕序号比较（spec 2062 / T3225–T3226 / R63）

纵切片：`SequenceOrder`（core/recovery 主）+ `SequenceOrderTest`
（六用例）。回绕安全差序、半环诚实界、距离折算。

- 测试：`mvn -pl buzhou-core test -Dtest=SequenceOrderTest` 6/6 绿。
- 教训入档：连「病证断言」都要先手推——反转病的方向（谁被判前）
  写反即假绿风险。
