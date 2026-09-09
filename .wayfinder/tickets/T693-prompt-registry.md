---
Type: task
Status: closed
---
## Question

`PromptVersion` 不可变快照 + `PromptRegistry` 接口（publish 单调/latest
自动指针/label 重指/按版钉取/未知 fail-fast）+ `InMemoryPromptRegistry`。

## Resolution

done（2026-09-08）：impl-374；版本单调 + 标签晋级回滚 + 钉版 + fail-fast
用例绿。
