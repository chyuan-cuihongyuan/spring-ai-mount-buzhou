# 620 — 熔断时间窗衰减

> 借鉴：[resilience4j](https://github.com/resilience4j/resilience4j) CircuitBreaker TIME-based sliding window。
> 来源：F 会话第 21 轮 = effort #600 / [T890](../../.wayfinder/tickets/T890-circuit-timewindow-shape.md) / [T891](../../.wayfinder/tickets/T891-circuit-timewindow-verify.md) / impl 473。

## 背景

熔断计数窗（spec 15）是 count 环形窗：低频调用下陈年失败永久占窗——半小时前的失败仍参与下一次跳闸判定， provider 早已恢复却继续被跳。

## 目标

`circuit.time-window`（默认 0 = 零变化）：正时长时老样本出率计算与 min-calls 门。

## 非目标

- 不做时间桶聚合（时间过滤即可达意，实现小）。
- 不改跳闸/复位/半开/共享闸语义。

## 设计

ring 窗保留（容量语义不动）+ 平行时间戳 ring（注入 Clock）；rate 与样本门按时间过滤新鲜样本计算。

## 测试

3 用例：陈旧出窗不跳 / count 窗零变化 / 窗内新失败照跳。

## 兼容性

Circuit record 扩组件 + 8/7/6 参兼容构造（canonical 已有 @ConstructorBinding）；默认 0 完全直通。
