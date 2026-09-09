# Wayfinder Map — Buzhou 数据集期望套件（effort #112，A 会话第 7 轮）

> A 侧编号策略沿用 #111 声明（#111+ / 偶数 spec / T451+）。B 侧预告主题避让。

## Destination

评估 run 前的数据合约：声明式期望套件对数据集行面 fail-fast——脏数据不进
评估器、不以畸形分数晚暴露。

## Notes

- 借鉴 Great Expectations；只读校验不修数；发现样本封顶 10 + 「共 N 处」
  诚实计数；summary 单行 CI 可读；空套件恒过（零期望 = 零意见）。

## Decisions so far

- [DatasetExpectations](../tickets/T459-dataset-expectations.md) — 四内置期望
  （非空输入/期望在场/输入唯一/规模窗）+ named() 自定义行级 + 行/数据集两级。

## Not yet specified

- EvalRunner 接线（run 前自动校验 + 越过开关）；期望套件随数据集持久化。

## Out of scope

- 跨字段/统计分布期望（均值/方差窗口）；自动修数。

## Tickets

- [x] [T459 数据集期望套件](../tickets/T459-dataset-expectations.md)（impl-279）
- [x] [T460 收口提交](../tickets/T460-dataset-expectations-close.md)（impl-279）
