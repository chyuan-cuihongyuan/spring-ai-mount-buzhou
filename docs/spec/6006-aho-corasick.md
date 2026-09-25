# Spec 6006 — Aho-Corasick 自动机（effort #6006，T7）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6213–T6214，impl 2207）。
> 借鉴：Aho-Corasick 多模式匹配论文思想（ripgrep/安全扫描器同源）。

## Problem Statement

多模式串匹配的病：每模式独立 indexOf 扫（O(模式数×文本长)，
模式集大时放大失控）——**一次扫描命中全部模式面**缺失。

## Solution

`AhoCorasick`（core/metrics）：

- Trie + 失配链（BFS 构造）：当前节点失配时沿 fail 回退，
  后缀状态全复用——文本**单次扫描**命中所有模式（O(文本长
  +命中数)）；
- 输出 canonical 序：按起始位置升序、同位按模式文本字典序（重复模式并列）
 （确定性）；Match(pattern,start,end) end 排他；
- fail-fast：null/空模式、null 文本。

## User Stories

1. 作为扫描作者，敏感词集一次扫描全命中——模式集大不放大。
2. 作为审计作者，同文本同模式集同输出序——确定性可回放。

## Testing Decisions

- 经典 ushers×{he,she,his,hers} 三命中钉住；重叠命中
 （aaaa×aa=3）钉住；30 模式×400 文本扰动 vs 逐位置逐模式
 暴力圣像全等；中文模式集钉住；fail-fast。

## Out of Scope

- 不做流式逐字符回调（一次性 scan 面）；不做通配/正则；
  不做并发加锁。

## Further Notes

- 与 KmpSearch（Q 系）同族不同面：单模式前缀函数 vs 多模式
  自动机一次扫；与 XorFilter（S43）不同面：成员判定 vs
  位置命中。
- 里程碑：T7/50（14%）。
