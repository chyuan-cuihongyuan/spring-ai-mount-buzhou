# 601 — 模型离群驱逐恐慌阈值

> 借鉴：[envoyproxy/envoy](https://github.com/envoyproxy/envoy) outlier detection `panic_threshold`——健康上游跌破占比阈值时路由忽略驱逐（可用性优先）。
> 来源：F 会话第 2 轮 = effort #600（600 系 50 轮自迭代）/ [T852](../../.wayfinder/tickets/T852-outlier-panic-shape.md) / [T853](../../.wayfinder/tickets/T853-outlier-panic-verify.md) / impl 454。

## 背景

spec 149 的离群驱逐按连错逐出坏端点，但**全逐会导致降级链直接空转**——所有备模型都在逐出窗口时，`filter` 返回空列表，调用方反而失去一切选择。Envoy 对集群有 panic threshold：健康主机占比跌破阈值（默认 50%）时忽略驱逐照常路由——「全逐比试坏端点更糟」。

## 目标

`ModelOutlierEjection.filter` 在剔除后健康候选数 < ceil(候选数 × panicThresholdPercent / 100) 时**忽略驱逐返回全量候选**，并计数 + WARN 留痕。

## 非目标

- 不做驱逐比例上限（max-ejection-percent，驱逐侧封顶）——雾区另议。
- 不改驱逐判定/窗口/复池语义（spec 149 口径零变化）。

## 设计

- `Config` 第三参 `panicThresholdPercent`（[0,100]；两参构造兼容 = 0 关闭，`defaults()` 不变——**默认零行为变化**）；`withPanicAll` 便捷预设 = 100。
- 阈值下限向上取整（3 候选 × 50% → 2）；**严格低于**才触发（恰在阈值不触发）。
- 触发路径：`buzhou.outlier.panic` 计数 + WARN（健康数/候选数/阈值/percent 全留痕）+ 返回原候选列表（含在逐成员）。
- 空候选列表不触发（返回空，语义不变）。

## 测试

8 用例（见 [T853](../../.wayfinder/tickets/T853-outlier-panic-verify.md)）：默认关闭回归、全逐返回全量、阈值边界取整、恰在阈值、空候选、参数校验、兼容构造语义。

## 兼容性

- Config record 扩组件 + 保留两参构造：源码兼容（canonical 构造调用处需加参——仓内仅测试与 defaults()，已同步）。
