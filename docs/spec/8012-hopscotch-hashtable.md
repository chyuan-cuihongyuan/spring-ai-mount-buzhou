# Spec 8012 — HopscotchHashTable 跳房子哈希（effort #8012，V13）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8025–V8026，impl 2314）。
> 借鉴：Herlihy, Shavit & Tzafrir 2008（U 系 Wave 3/6 两度退雾区遗珠——本轮认领）。

## Problem Statement

开放定址的病：线性探测聚集化（cluster 拖长探测链），
**跳房子用「邻域 H 槽内必可达」不变量把探测距离钉死在
常数 H 内**——缓存友好 + 摊还 O(1)。

## Solution

`HopscotchHashTable`（core/cache）：桶数组+每桶 H 位邻域
位图（hop info）；插入先线性探测找空位，超邻域则沿空位
回跳（swap 链）把元素搬进邻域；邻域满触发扩容（×2）；
`put` upsert 覆值不增位/`get` 缺席 null 诚实/`remove` 缺席
fail-fast/`size` 读数；null 键 fail-fast；装载上限 0.9
（硬编码纪律常数——明示）；单线程面（并发变体不在本件）。

## Testing Decisions

- 手锚（put/get 覆值/删除再插）；不变量钉住：任意键命中
  位置 ∈ 其家桶邻域 H 内（200 随机操作后全量校验 hop 位图
  一致）；1000 随机操作 vs HashMap 圣像键值全等 + 扩容
  触发重现（小 H=4 构造聚集）；fail-fast。

## Out of Scope

- 不做并发锁变体（Herlihy 论文并发面——本件为顺序语义
  地基，明示）；不做删除墓碑（swap 链直接腾位）。

## Further Notes

- 与 ExtendibleHashing（T 系）同族不同面：目录倍增 vs
  邻域不变量常数探测。
- 里程碑：V13/50（26%）。
