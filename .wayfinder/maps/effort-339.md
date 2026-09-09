# Wayfinder Map — Buzhou 多模型加权路由装配收尾（effort #339，C 会话第 40 轮）

> C 会话第 40 轮（40/50）。199 立了平滑加权路由器（Nginx smooth WRR）
> 至今零消费——「按权重把流量分给多个模型」的 LiteLLM Router / OpenRouter
> 场景没有装配面：宿主要有 cost/capacity 混合路由（70% 便宜模型 + 30%
> 强模型）只能自建。

## Destination

`WeightedChatModel`（ChatModel 装饰器：平滑加权逐调用选路——比例精确
时间平滑 5:1:1 不连五爆发；199 原语的消费兑现；stream 诚实委派同路；
计数 buzhou.routing.routed{model}）+ yml 面
`buzhou.routing.weights.<beanName>=<int>`（≥2 项才装配 @Primary——
宿主多 ChatModel bean 时路由器成为主模型；缺名启动红带修法）。

## Notes

- 号段：spec 339 / T669–T670 / impl-362。
- 借鉴源：LiteLLM Router / OpenRouter（多模型加权分流）+ Nginx smooth WRR（199 原语）。
- 纪律：未配 weights / 只配 1 项 = 零变化（无路由 bean）；路由透明——
  Prompt 原样透传（模型特定 options 兼容归宿主，与 137 对冲同口径）。

## Decisions so far

- 以 bean 名寻址候选（ChatModel 无自报名面）——beanName 既是 yml 键
  也是计数 tag。
- @Primary 只在路由器装配时标注——宿主显式 @Primary 冲突时启动红
  （两个 primary 不允许——诚实失败优于静默遮蔽）。

## Out of scope

- 按延迟/错误率自适应调权（健康联动后续轮）；粘性会话路由（同会话
  同模型——记忆连续性权衡另议）；模型特定 prompt 改写。

## Tickets

- [x] [T669 WeightedChatModel 平滑加权选路](../tickets/T669-weighted-model.md)
- [x] [T670 yml 装配 + 收口](../tickets/T670-routing-assembly.md)
