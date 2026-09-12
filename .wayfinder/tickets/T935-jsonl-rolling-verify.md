---
id: T935
title: JSONL 轮转的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T934
created: 2026-09-13
---

## Question

轮转真的发生且代际正确？行不丢不半截？默认与显式关两种口径都钉住了？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 43 轮）：① 小阈值 @TempDir 用例：超限即生成 file.1、原文件重开只含新行、每行完整一个 JSON；② 连续多轮：.1/.2/.3 按代保留、.4 不存在（maxHistory=3 封顶）；③ 0=关：无轮转文件；④ PromptUsage 4 参重载轮转生效；⑤ HealthTimeline/Shadow 既有用例零回归（64MB 阈值下测试体量不触发轮转）。`mvn -pl buzhou-core -am test` + `-pl buzhou-resilience -am test` 全绿。
