# Spec 339 — 多模型加权路由（effort #339）

> wayfinder map：`.wayfinder/maps/effort-339.md`（T669–T670）。C 会话第 40 轮，
> 199 原语装配收尾。

## Problem Statement

平滑加权路由器（spec 199）已落地却零消费：宿主的「70% 流量给便宜
模型、30% 给强模型」混布（LiteLLM Router / OpenRouter 的经典场景）
没有装配面，只能自建路由或手动切模型。

## Solution

- **`WeightedChatModel`**（buzhou-resilience，ChatModel 装饰器）：
  逐 call/stream 经 `WeightedRouter.pick()`（Nginx smooth WRR——比例
  精确、时间平滑、同序可复现）选一路模型，Prompt 原样透传（模型特定
  options 兼容归宿主——137 对冲同口径）；stream 与 call 同路选取
  （一次调用一个模型，绝不混流）；计数 `buzhou.routing.routed`
  （tag model=beanName）+ `routes()` 观测面。
- **装配**：`buzhou.routing.weights.<beanName>=<int>`（正整数）——
  ≥2 项才建 `@Primary WeightedChatModel` bean（按名取 ChatModel bean；
  缺名启动红带修法；权重非法红）。未配/单项 = 零变化。
- 宿主已有显式 @Primary 模型 bean 时与路由器 @Primary 冲突 → 启动红
  （诚实失败优于静默遮蔽）。

## User Stories

1. 作为成本敏感宿主，我想 70/30 分流便宜与强模型，所以 平均成本降
   而能力面仍在。
2. 作为运维，我想流量平滑不连五爆发（5:1:1 场景），所以 慢模型不被
   连续打、快模型不闲置。
3. 作为运维，我想路由分布有计数可查，所以 权重配错能被发现。
4. 作为使用者，我不想配路由时模型装配与现状完全一致，所以 升级
   零风险。
5. 作为宿主，我想 yml 写错 bean 名启动即红，所以 拼写错误不会变成
   「路由到不存在的模型」。

## Implementation Decisions

- 候选以 bean 名寻址（ChatModel 无自报名面）；beanName 兼作计数 tag。
- stream 与 call 各自独立 pick（两次 stream 各自选路——WRR 序列推进；
  不做同 prompt 粘性）。

## Testing Decisions

- 路由分布：3:1 权重 8 次 call 恰 6:2（smooth WRR 确定性——同序可
  复现）；stream 同路选取；单候选直通。
- 装配：双 bean + weights → @Primary 路由器在且计数 tag 对；未配/
  单项无 bean；缺名启动红；权重 0/负红。

## Out of Scope

- 自适应调权（延迟/错误率联动）；会话粘性；模型特定 prompt 改写；
- 跨实例权重一致性（WRR 状态进程内——多实例分布自然均衡）。

## Further Notes

- 新公共类型 `WeightedChatModel` + `BuzhouRoutingProperties` 随轮
  regenerate 快照。
- 与 301 对冲互补：对冲管「单调用长尾押注」，路由管「流量面成本分配」。
