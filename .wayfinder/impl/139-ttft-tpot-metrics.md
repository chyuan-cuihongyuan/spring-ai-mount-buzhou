# 139 — TTFT/TPOT 流式指标

**Parent:** spec 46 §A / [T170](../tickets/T170-ttft-tpot-metrics.md)

**What to build:** 流式模型调用在首个内容信号到达时记 TTFT（span 属性 + STREAM_FIRST_TOKEN 事件 +
`buzhou.model.ttft` timer），流完成时按 usage 记 TPOT（span 属性 + `buzhou.model.tpot` timer）；
非流式路径零变化；未装 micrometer 零开销。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] 「首内容信号」口径：正文/思维链累计器空→非空 或 首个工具调用 delta 信号；空块（usage-only/role-only）不触发
- [ ] TTFT 计时自订阅建立（doOnSubscribe）；span 属性 `ttft.ms` + EventType 新内置常量 `STREAM_FIRST_TOKEN` 事件恰一条
- [ ] TPOT 仅 completionTokens>1 且有 TTFT 时记：`(总时长−TTFT)/(completionTokens−1)`，span 属性 `tpot.ms`
- [ ] `buzhou.model.ttft` / `buzhou.model.tpot` 预注册（BuzhouMetricsBinder 无 tag 基型）+ 记录侧 model.name 截断 64
- [ ] 伪流式序列断言：先空块后内容块 TTFT 计内容块时刻；completion≤1 或无首内容不记 TPOT/TTFT
- [ ] buzhou-observability 模块 `mvn verify -am` 全绿
