# Spec 206 — 预算批量存入（effort #147）

> wayfinder map：`.wayfinder/maps/effort-147.md`（T570–T571）。

## Solution

`RetryBudget.deposit(int calls)`：批量路径（聚合层/批处理）一次记账 n 次调用
——与循环逐次 deposit 等价；0 次 no-op；负数 fail-fast。
