# Spec 8004 — GlobMatcher 通配符匹配（effort #8004，V5）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8009–V8010，impl 2306）。
> 借鉴：POSIX fnmatch/bash glob 思想。

## Problem Statement

路径/名称匹配的病：正则过重且语义漂移——**glob 的四类
原语（`*` 任意含空/`?` 恰一/`[...]` 字符类/字面量）才是
配置面的事实标准**，而朴素递归回溯在最坏路径上指数。

## Solution

`GlobMatcher`（core/metrics，静态工具面）：模式前端解析为
四类 token（星/问/字符类/字面量——类支持 `a-z` 区间与
`!` 否定、首 `]` 字面量）+ 星号单候选位回溯线性匹配
（最坏 O(nm) 有界，非指数）；`?` 恰一字符；`*` 跨目录边界
语义不做特殊化（POSIX glob 无 `**` 特例——明示）；未闭合
`[`/null fail-fast；确定性纯函数。

## Testing Decisions

- 手锚（*.java、a?c、[a-c]x、[!a]x、首 ] 字面量 []a]x、
  `*` 含空串、未闭合 [ 拒绝）；300 随机（a/b/*/? 模式域）
  vs 递归暴力圣像判定全等；fail-fast。

## Out of Scope

- 不做 `**` 跨段特例；不做 `{a,b}` 花括号展开。

## Further Notes

- 与 WildcardStringMatcher（若既有）同族不同面：本件为
  fnmatch 全原语面（字符类+否定+区间）。
- 里程碑：V5/50（10%）。
