# impl 1559 — 重试主机排除（spec 2008 / T3117–T3118 / R9）

纵切片：`RetryHostExclusion`（buzhou-resilience routing 主）+
`RetryHostExclusionTest`（七用例）。冷却让位、全排除回退、序保持、
excludedCount 显形。

- 测试：`mvn -pl buzhou-resilience test -Dtest=RetryHostExclusionTest` 7/7 绿。
- 教训入档：全排除回退语义下单候选断言必写回退期望——「排除是偏好
  不是硬门」要先入 spec 再写用例。
