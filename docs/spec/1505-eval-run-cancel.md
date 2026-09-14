# 1505 — EvalRunner 评估 run 协作式取消

> 来源：M 会话第 6 轮 = effort #1505（impl 1108）。Kubernetes Job 删除传播语义（在飞完成、未启动不再启动）。

## 背景

`EvalRunner.run` 开始后只能跑完全程或等自动止损（失败率剪枝 spec 901 / 预算闸 spec 520）——宿主发现数据集配错、方向不对时无「立即止损」通道，在飞大 run 白烧算力与模型费。

## 目标

- `requestCancel()`：实例级协作取消（volatile 标记）；run 开始时清零（上轮残留不污染新 run）；
- 项边界生效：串行与并行两路径统一——剩余项标新状态 `cancelled`（与 `pruned` 的失败率止损语义分立）；在飞项做完；
- 已完成项结果保留、run 照常落盘（可分析已完成部分）；cancelled 项不进 pass/fail/error 任一桶；
- 指标 `buzhou.eval.run.cancelled`。

## 兼容性

纯新增（新方法 + 新状态常量）；未调用 requestCancel 时行为零变化。
