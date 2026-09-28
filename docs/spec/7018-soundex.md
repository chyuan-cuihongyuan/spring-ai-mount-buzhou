# Spec 7018 — Soundex 语音编码（effort #7018，U19）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7237–U7238，impl 2270）。
> 借鉴：美国 1880 年人口普查 Soundex（NARA 规范）。

## Problem Statement

同音异形检索的病：精确拼写匹配搜不到发音相近变体
（Robert/Rupert）——**发音部位归并编码面**缺失。

## Solution

`Soundex`（core/metrics，静态工具面）：辅音按发音部位
映射数字、首字母保留、相邻同码折叠（元音断开、H/W
不断开——Ashcraft→A261 经典钉子）、补零截断 1+3；
非字母丢弃、小写归一；纯函数全规则确定性。

## Testing Decisions

- NARA 官方向量八例；归一化/补零/截断；fail-fast。

## Out of Scope

- 不做变体方言（含 NARA 撞号歧义项差异）。

## Further Notes

- 与 KmpSearch（3014）同族不同面：精确匹配 vs 发音归并。
- 里程碑：U19/50（38%）。
