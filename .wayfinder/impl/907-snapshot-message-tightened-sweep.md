# 907 — SnapshotMessage 补测与跨模块复核（R5）

**What to build:** SnapshotMessageTest（compact 构造 null 防御 + 防御拷贝）；六小模块 miss≥1 口径隔离复扫清单。

**Blocked by:** None — can start immediately.

**Status:** in-progress

- [x] core：SnapshotMessageTest（2 用例：null→空 Map / 防御拷贝 + 字段透传）
- [x] spec 1204 + README 行
- [ ] 六小模块（tools/observability/observe-otel/observe-dashboard/spill/resilience）隔离复扫清单入 map

## Done

（收口时填写）
