# Spec 9020 — Suffix Automaton 后缀自动机（effort #9020，W21）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9041–W9042，impl 2373）。
> 借鉴：后缀自动机（Blumer 1985——SAM 在线构造，CP-Algorithms 同源）

## Problem Statement

子串查询逐次扫描 O(nm)、后缀数组需离线重排——
**SAM**：最小 DFA 恰接受全部子串，O(n) 状态
在线构造。

## Solution

SuffixAutomaton（core/metrics，实例类）：of(text)
构造；containsSubstring/distinctSubstringCount/
substringFrequency/substringCountByLength/stateCount；
ASCII 字节域明示。

## Testing Decisions

成员九点锚；互异锚 3/6/5；aaaa 频次梯度；
30 随机文本互异数+频次逐一对拍暴力；状态数
≤2n+1；fail-fast。

## Out of Scope

不做 Unicode 全域（ASCII 明示）；不做第 k 子串
字典序（序面归 SuffixArray）；不做多文本广义 SAM。

## Further Notes

与 SuffixArray 同域不同面。Wave 4 第三件。
