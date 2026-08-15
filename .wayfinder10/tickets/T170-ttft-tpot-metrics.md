---
Type: task
Status: open
---
## Question

ObservabilityAdvisor adviseStream 首信号打点：TTFT Timer（buzhou.llm.ttft）+ TPOT（总时长/输出 token）+ stream 总时长；span 发 model.first-token Event（带毫秒值）；非流式不计。LiteLLM/vLLM 双源口径（timer 名对齐业界语义）。验证：流式单测断言 meter 与 Event 落点。
