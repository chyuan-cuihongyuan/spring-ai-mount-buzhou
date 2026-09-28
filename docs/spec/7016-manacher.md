# Spec 7016 — Manacher 最长回文（effort #7016，U17）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7233–U7234，impl 2268）。
> 借鉴：Manacher 1975 线性回文思想。

## Problem Statement

最长回文的病：逐中心扩展 O(n²)（长串放大）——**镜像
复用右界线性面**缺失。

## Solution

`Manacher`（core/metrics，静态工具面）：

- 插入分隔符统一奇偶 + p[i]≥min(p[mirror], right−i)
  镜像起步扩展 O(n)；最长回文子串并列取起点最小者
  （canonical——同串同结果）；
- 分隔符 '#'：输入含 '#' fail-fast（变换唯一性前提，
  明示拒绝而非静默错配）；null/空串 fail-fast。

## User Stories

1. 作为文本作者，线性最长回文底座。
2. 作为审计作者，随机串 vs 暴力圣像（长+canonical）全等。

## Testing Decisions

- 手锚奇偶（babad/cbbd/geeksskeeg）；300 随机串 vs
  暴力圣像（长度+canonical 串全等）；fail-fast 三路。

## Out of Scope

- 不做回文计数面；不做流式回文。

## Further Notes

- 与 KmpSearch/ZArray（同包）同族不同面：前缀函数/匹配
  段 vs 回文半径镜像复用。
- 里程碑：U17/50（34%）。
