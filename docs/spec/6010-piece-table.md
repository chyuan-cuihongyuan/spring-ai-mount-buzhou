# Spec 6010 — Piece Table 文本缓冲（effort #6010，T11）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6221–T6222，impl 2211）。
> 借鉴：Piece Table 思想（VSCode/Word 文本缓冲同源）。

## Problem Statement

编辑器文本缓冲的病：每编辑复制全文（O(n) 编辑放大，大文档
卡顿）或双栈复杂（gap buffer 移动成本）——**原稿不可变+
增量片表面**缺失。

## Solution

`PieceTable`（core/fs）：

- 原稿只读 + 追加型增量缓冲（added 只存新字符——编辑不
  复制原文）；片段表按序拼装视图（insert/delete 只增删/
  切分片段，不搬原文）；
- insert 越界/参数畸形 fail-fast；delete 跨片段边界裁剪；
- 读数：length/pieceCount/addedLength（增量可见——结构
  不说谎）；text() 拼装视图。

## User Stories

1. 作为编辑器作者，大文档万字插入零全量复制——增量缓冲。
2. 作为撤销作者，addedLength 只增不减可见——审计增量。

## Testing Decisions

- 固定种子 500 混合编辑 vs StringBuilder oracle 全等；
  尾部追加/中点切分/跨片段删除逐值钉住；addedLength=累计
  插入字符数（不含原文）；pieceCount 有界；fail-fast。

## Out of Scope

- 不做撤销栈/红黑片表优化（J. Fine 增量面）；不做并发
  加锁；不做持久化。

## Further Notes

- 与 RopeBuffer（spec 4044）同族不同面：权重树拼接 vs
  原稿+片表双缓冲；与 RollingJsonlWriter（fs）不同面：
  行追加 vs 位置编辑。
- 里程碑：T11/50（22%）。
