# impl-97 — dashboard 消费会话索引

**What to build:** 过滤会话列表（索引优先/观测回退降级可感）。

**Blocked by:** None

**Status:** done

- [x] DashboardQueryService.listSessionsFiltered + IndexedSessionPage（fromIndex 降级标记）
- [x] DashboardModule.Builder.sessionIndex 可选注入
- [x] 测试：索引过滤/分页探测 + 回退降级——dashboard 23/23 绿；spec 36 §B

## Done

commit：见 git log（impl-97）。
