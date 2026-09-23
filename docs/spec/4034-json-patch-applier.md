# Spec 4034 — JSON Patch 应用（effort #4034，R35）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6069–T6070，impl 2135）。
> 借鉴：RFC 6902 JSON Patch（+RFC 6901 JSON Pointer）。

## Problem Statement

配置/状态文档的增量化编辑病：全量覆盖（大文档重传、并发
互踩）或自造 diff 语义（无标准、不可审计）——**标准增量
操作面**缺失。

## Solution

`JsonPatchApplier`（core/policy，Jackson JsonNode 承载）：

- 六操作全量：add/remove/replace/move/copy/test（RFC 6902
  §4 语义逐条）；
- JSON Pointer（RFC 6901）：`""` 根、`~0`/`~1` 转义、数组
  数字下标、add 的 `-` 追加语义；
- 原子性：先深拷贝应用，任一操作失败整patch失败上抛
 （IllegalArgumentException 带操作序号与原因），原文档不变；
- move 的数组下标记账（先 remove 后 add 的移位诚实处理）；
- test 失败即patch失败（条件前置语义）。

## User Stories

1. 作为配置热更新作者，增量下发可审计（操作记录即变更日志）。
2. 作为会话状态编辑作者，test 前置守卫 + 原子应用——半截
   变更不可见。

## Testing Decisions

- 六操作各正反例（add 数组移位/- 追加、replace 缺路径错、
  move 下标记账、test 失败整patch拒）；转义 `~0/~1` 双向；
  非法指针（坏转义/非数字下标/remove `-`）fail-fast；原子性
  （失败后原文档 equals 原值）。

## Out of Scope

- 不做 JSON Merge Patch（RFC 7386）；不做 patch 生成
 （diff 面归三方合并件）；不做 XML Patch（RFC 5261）。

## Further Notes

- 与增量+基准帧编码（spec 4008，数值列 delta）同族不同面：
  文档操作增量 vs 数值编码增量。Wave 6 收口件。
- 里程碑：35/50。
