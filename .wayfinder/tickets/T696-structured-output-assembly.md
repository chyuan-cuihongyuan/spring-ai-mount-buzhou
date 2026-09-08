---
Type: task
Status: closed
---
## Question

`buzhou.resilience.structured-output.{enabled,max-repair-attempts,schema}}`
独立 RuntimeConfig bean 装配；enabled 无 schema fail-fast；E2E 经
ScriptedChatModel 全链。

## Resolution

done（2026-09-08）：impl-375；E2E + ApplicationContextRunner 装配/fail-fast
用例绿，buzhou-resilience 全模块绿。
