# Spec 6020 — Geohash 地理哈希（effort #6020，T21）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6241–T6242，impl 2221）。
> 借鉴：Redis GEO/位置服务 geohash 思想。源码于 T18 对账批预入档。

## Problem Statement

位置邻近判断的病：两点阈值邻近需逐对计算（无法用索引前缀
剪枝）——**空间前缀编码面**缺失。

## Solution

`Geohash`（core/policy，源码已预载）：

- 经纬交替二分（经度偶数位起），5 位一字符 Base32——
  前缀共享即空间邻近（同格/邻格共享长前缀）；
- decode 还原包围盒+中心（encode/decode 互逆定构）；
- fail-fast：lat/lon 域外、精度越域、null/非法字符。

## User Stories

1. 作为位置作者，POI 前缀索引邻近剪枝——GEO 底座。
2. 作为审计作者，同点同精度同串——编码可回放。

## Testing Decisions

- Wikipedia 经典锚 ezs42（42.605,−5.603）钉住；200 随机点
  互逆性（bbox 含点+中心再编码同串）；包围盒随精度收缩；
  前缀共享邻近性；fail-fast。（初版锚值 7zzz 系记忆错误，
  边界归上半手推复核改 s 锚。）

## Out of Scope

- 不做邻居格枚举；不做 MIME/Base32 变体；不做多点聚类。

## Further Notes

- 与 ZOrderCurve（recovery）同族不同面：经纬网格串 vs
  平面填充序。
- 里程碑：T21/50（42%）。
