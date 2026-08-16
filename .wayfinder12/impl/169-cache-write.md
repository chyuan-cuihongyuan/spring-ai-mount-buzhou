# 169 — 写入边界（终态判定）

**Parent:** spec 53 §B / [T204](../tickets/T204-cache-write.md)

**Status:** done

- [x] isTerminal 公开钉住语义：无 toolCalls 且内容非空才写（本地裁定——LiteLLM 无此约束，
  agent harness 工具副作用安全必须有）
- [x] 异常/空/toolCalls 一律不写（cacheIfTerminal 前置判定）
- [x] 单元断言：toolCalls 响应 / 空白内容 / 终态三分判定
- [x] 端到端 toolCalls 对抗（注册工具的替身）进 T208 红队
