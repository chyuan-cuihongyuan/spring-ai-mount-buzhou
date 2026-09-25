---
id: T6213
title: T 会话 T7 Aho-Corasick 自动机的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

多模式串匹配怎么单次文本扫描全命中？（spec 6006 /
effort #6006 / T7）

## Resolution

**AhoCorasick（core/metrics）**：Trie+BFS 失配链——文本一次
扫描沿 goto/fail 走，后缀状态全复用（O(文本长+命中数)）；
Match(pattern,start,end) canonical 序（起始升序+模式注册序）；
null/空模式与 null 文本 fail-fast。
