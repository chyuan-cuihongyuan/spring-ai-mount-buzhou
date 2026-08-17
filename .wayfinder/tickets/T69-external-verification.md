---
Type: research
Status: closed
assignee: main
blocked-by:
---

## Question

外围收口六条线（dashboard 管理端安全、observability 管线生命周期、MCP client 运维、工具沙箱强化、配置元数据工程、redteam 目标真实性）在 GitHub stars ≥ 10K 的开源项目里分别对应的既定实践是什么？逐条核验并给出可采纳清单：哪些直接采纳、哪些因不达标注记为不做。产出写入 docs/research/oss-perimeter-hardening.md。

## Resolution

研究完成，产出 [docs/research/oss-perimeter-hardening.md](../../docs/research/oss-perimeter-hardening.md)。六条线关键结论：①dashboard 对齐 Actuator 安全模型（默认 127.0.0.1、非 loopback 必须带 token、500 不回显）；②观测管线 bean 化 + MeterBinder 预注册单一口径；③MCP 危险工具按客户端侧动词模式分类登记（勿信 server 元数据）；④run_command 采纳 OpenHands 三要素（超时/取消同收口杀进程树、env 白名单、读入上限+告知 agent）；⑤必须补 spring-boot-configuration-processor 否则 impl/43 元数据是死文件；⑥redteam target 采纳 `{output, guardrails:{flagged}}` 契约 + 响应头派生 + `type: guardrails` 断言。不达标注记：micrometer/MCP SDK/promptfoo 本体 star 不足，经 Boot（75K+）/LangChain（100K+）/官方文档间接采纳。T70–T79 由此展开。
