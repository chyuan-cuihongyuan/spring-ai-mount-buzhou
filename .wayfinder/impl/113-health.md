# impl-113 — health 新维度

**What to build:** outbox 水位 + 索引装配态两个健康面（条件装配）。

**Blocked by:** None

**Status:** done

- [x] WebhookOutboxHealth（pending/deadLetters/delivered/dropped；恒 UP）
- [x] SessionIndexHealth（wired/hasRows 采样探测；查询故障可见不 DOWN）
- [x] auto-config 条件注册 + 装配断言（details 键）；core 4/4 绿；spec 39 §C

## Done

commit：见 git log（impl-113）。
