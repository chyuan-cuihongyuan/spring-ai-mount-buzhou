---
Type: task
Status: closed
assignee: zcode
blocked-by: T101
---
## Question

知识库同步与 effort #5 收口：.Knowledge topics 是否需要新增/更新（新能力语义进知识库，走 f2s-kb-sync 语义：大纲→写入 topics/index/manifest——本仓知识库当前只含 flow2spec 流程 topics，核实后决定是否扩业务主题）、README 九大机制表述更新（韧性/成本/fork/webhook 等新能力如何呈现）、CONTEXT.md 术语增补、MAP Decisions so far 完整回填、effort #5 闭合声明。产出 impl 77。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **.Knowledge 不扩业务主题**：核实知识库现只含 flow2spec 流程 topics（manifest-routing 6 条全流程类）——其定位是「流程/规则知识库」，业务机制语义已由 `docs/spec/` 00–23 + `api-surface.md` + javadoc 三层承载；双处维护同一语义无净收益且必漂移。**决策：不新增 topics**（若未来 Codex 侧问答频繁下钻业务源码，可再走 f2s-kb-distill 增量补）。
2. **README**：新增「生产级纵深（effort #5）」段——10 项新能力表（能力/一句话/详设链接）+ runbook/api-surface 指引；九大机制表保持不变（新能力是运营纵深非第十大机制）。
3. **CONTEXT.md**：增「韧性与成本」术语节 9 条（熔断/降级链/预算/配额/REASK/fork/webhook/漂移等）。
4. **MAP**：Decisions so far 回填 T81–T102 全部 22 条；Not yet specified 清空（4 项转收口记录「后续 effort 候选」）；Tickets 节闭合声明；收口记录落档（含过程教训）。
