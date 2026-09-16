# Spec 2046 — ETag 条件请求匹配（effort #2046，R47）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3193–T3194，impl 1597）。
> 借鉴：HTTP RFC 7232——If-None-Match/If-Match 与强弱 ETag。

## Problem Statement

查询/导出端点每次全量回包：客户端已持有同版数据仍重复拉（带宽与
生成成本双耗）；写侧乐观并发无前置检查（他人已改则覆盖）。HTTP 已
有标准答案——条件请求，语义缺原语。

## Solution

`EntityTagMatcher`（core/webhook，纯函数零状态）：

- 强比较 `strongMatch`：全等且双方非弱（字节等价——If-Match 用）；
  弱比较 `weakMatch`：剥 W/ 前缀后全等（语义等价——If-None-Match 用）；
- `ifNoneMatchHit(header, current)`：头 "*" 通配或候选任一**弱比较**
  等即命中（→ 调用方回 304 Not Modified 省全量载荷）；
- `ifMatchSatisfied(header, current)`："*" 或候选任一**强比较**等即
  满足（未满足 → 412 Precondition Failed 乐观并发防护）；头 null/空
  恒 false（RFC：If-Match 缺失不做检查——调用方区分）；
- 逗号列表空格容忍；契约：etag 非 null fail-fast。

## User Stories

1. 作为查询端点作者，If-None-Match 命中回 304——同版客户端零重复
   载荷。
2. 作为写侧，If-Match 未满足拒写——覆盖他人改动被前置拦截。

## Testing Decisions

- 强比较三态（弱不配强）；弱比较剥前缀；304 判定（精确/通配/列表
  弱比较命中/未命中）；If-Match 强门（弱不满足 → 412）；无头恒
  false；带空格列表解析；畸形四型 fail-fast。

## Out of Scope

- 不做 ETag 生成（校验和面归 SessionExportChecksum）；不接具体端点
  （接线归后续轮）。

## Further Notes

- 与续读令牌编解码（O-1834）互补：那管分页续读，这管版本复用与
  并发防护。
