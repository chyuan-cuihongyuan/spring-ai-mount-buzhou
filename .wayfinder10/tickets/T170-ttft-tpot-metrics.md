---
Type: task
Status: closed
---
## Question

ObservabilityAdvisor adviseStream 首信号打点：TTFT Timer（buzhou.llm.ttft）+ TPOT（总时长/输出 token）+ stream 总时长；span 发 model.first-token Event（带毫秒值）；非流式不计。LiteLLM/vLLM 双源口径（timer 名对齐业界语义）。验证：流式单测断言 meter 与 Event 落点。

## Resolution

spec 46 §A / impl-139 落地：ObservabilityAdvisor adviseStream 订阅计时 + 首内容信号打点
（口径：正文/思维链累计器空→非空或工具调用 delta；usage-only/role-only 空块不触发）——
MODEL_CALL span 属性 `ttft.ms`、EventType 新内置常量 `STREAM_FIRST_TOKEN` 事件（恰一条）、
`buzhou.model.ttft` timer；流完成时 completion>1 且有 TTFT 记 `tpot.ms` 属性 +
`buzhou.model.tpot` timer（(总−TTFT)/(completion−1)）。BuzhouMetricsBinder 预注册两 timer
无 tag 基型；model.name 记录侧截断 64。非流式路径零改动。端到端 3 测试绿
（先空块后内容块 TTFT 计内容时刻 / 无内容不记 / completion=1 跳 TPOT）。
