# 947 — ElasticBudgetPool 并发守恒压测

> 来源：I 会话第 47 轮 = effort #947（impl 696）。G 会话 r47 压测轮同模式——池级单锁语义的并发正确性实证。

## 背景

`ElasticBudgetPool`（spec 157，Spark AQE 借鉴）：总容量 C + 各会话基础配额保底 + surplus 借用。并发借还下「预算不灭不失」守恒（Σheld + surplus == capacity）需压测实证。

## 目标

- 并发守恒：8 线程 × 500 次 tryAcquire/release 混合后 Σheld + surplus == capacity；
- base 保底：Σbase == capacity 时 surplus=0，拒绝超 base 借用（不吃他人 base）；
- 尝试数驱动验证（8×500=4000 次全部生效）。

## 兼容性

纯测试轮；零生产代码变更。
