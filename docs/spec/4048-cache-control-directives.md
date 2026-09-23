# Spec 4048 — Cache-Control 指令裁决（effort #4048，R49）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6097–T6098，impl 2149）。
> 借鉴：RFC 9111 §5.2 Cache-Control 指令语义（HTTP 缓存裁决）。

## Problem Statement

响应缓存的病：全凭自造 TTL 语义（指令被无视——no-store 挡
不住缓存、no-cache 变成不存）或裸字符串匹配（大小写/引号/
未知指令各行其是）——**标准指令解析与新鲜度裁决面**缺失。

## Solution

`CacheControlDirectives`（core/policy，嵌套 `Directives` 不另
立面）：

- `parse`：逗号分隔指令、大小写不敏感、值容忍引号、未知
  指令按 RFC 语义**忽略但记录**（unknown 读数）；负
  delta-seconds fail-fast（非 -1）；
- 优先级裁决 `judge(directives, age, shared)`：
  ① no-store → NOT_CACHEABLE（最高——存都不许）；
  ② no-cache → MUST_REVALIDATE（可存但每用必验）；
  ③ 新鲜度上限 = shared ? s-maxage ?: max-age : max-age；
  age < 上限 → FRESH，否则 STALE；
- 读数面：maxAge/sMaxAge/noStore/noCache/immutable/unknown；
- 确定性：同头同裁决。

## User Stories

1. 作为响应缓存作者，标准指令语义不被自造 TTL 覆盖。
2. 作为审计作者，同头同判（确定性可回放）。

## Testing Decisions

- 解析：大小写不敏感 + 引号值 + 未知指令忽略并记录 + 空
  头合法 + 负 delta fail-fast；裁决四象限（no-store 最高/
  no-cache 次之/共享缓存 s-maxage 压过 max-age/超龄 STALE）；
  确定性回放。

## Out of Scope

- 不做请求端指令合并（max-stale/min-fresh/only-if-cached
  的请求面）；不做 Vary 协商键；不做 warning 110/111。

## Further Notes

- 与精确响应缓存（spec 53，进程内 LRU+TTL）互补：彼件用
  本件裁决决定条目可用性语义。Wave 9 单件。
- 里程碑：49/50。
