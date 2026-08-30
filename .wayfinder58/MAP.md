# Wayfinder Map — Buzhou 会话归档冷层（effort #58，50 轮自迭代第 23 轮）

> effort #58，延续 #57（T361–T362 / impl-243）。主线：**#35 fog 项「会话归档冷层」**——
> SessionCleaner 是硬删除级联（retention 到期即清），删除前无冷存档安全网：误清
> 不可逆。

## Destination

`SessionArchiver`（cleanup 包）：`archive(sessionId)` 三槽快照（消息/摘要/state
→ 单归档 JSON 落 `__buzhou.archive__` 合成会话，键 `archive.<sessionId>`——fsck
天然豁免）→ SessionCleaner 级联删除；快照编码失败 fail-fast 不删（安全优先）；
空会话诚实 false；`restore(sessionId)` 原键原值回放三槽 + 删归档键；`archived()`
清单字典序。Instant 编解码经 databind SimpleModule（无 jsr310 依赖纪律）。

## Notes

- 借鉴：与 spec 28 导出/导入的差异定位——那是活迁移（Id 重映射），这是同环境
  冷存档（原键原值零转换）。

## Decisions so far

- 归档落 state store（不自建存储——既有五 store 面不扩）。
- 编码失败 fail-fast（宁可保留原会话不冒丢失风险——safe-by-default）。

## Not yet specified

- 归档 TTL/容量上限（archive 合成会话的清理调度）；压缩归档（gzip——体积另议）；
  autoconfig 接线（retention 到期先归档再删——策略开关另议）。

## Out of scope

- 沿用 #7–#57；跨环境迁移（spec 28 已有面）。

## Tickets

- [x] [T363 SessionArchiver 归档/回放/清单](tickets/T365-archiver.md)（impl-244）
- [x] [T364 3 例红队（三槽回放/空会话诚实/清单隔离）+ 收口](tickets/T366-archiver-close.md)
