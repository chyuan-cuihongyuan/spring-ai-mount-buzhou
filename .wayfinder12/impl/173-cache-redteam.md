# 173 — 缓存面红队对抗

**Parent:** spec 53 对抗面 / [T208](../tickets/T208-cache-redteam.md)

**Status:** done

- [x] 键注入：消息含序列化元字符（| } {）伪造边界不串键；同输入确定；模型名入键
- [x] TTL 过期不陈旧（advisor 层 Clock 注入链路）
- [x] 容量压挤热键存活（k0 持续访问免逐出；evicted 计数）
- [x] 命中重放语义（ChatResponse 只读共享；新包装由 advisor 保证——行为面 E2E 已钉）
- [x] 4 用例绿
