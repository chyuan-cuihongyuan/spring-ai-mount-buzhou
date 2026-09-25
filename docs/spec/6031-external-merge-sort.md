# Spec 6031 — External Merge Sort 外归并排序（effort #6031，T32）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6263–T6264，impl 2232）。
> 借鉴：Spark/数据库外部排序思想。

## Problem Statement

超内存数据排序的病：全内存排序 OOM——**切块游程+多路归并
面**缺失。

## Solution

`ExternalMergeSort`（core/fs，源码已预载）：

- ⌈n/chunkSize⌉ 游程：每窗内排序成有序游程，多路归并逐
  位取最小（并列游程序靠前——确定性）；
- 读数：sorted/runCount/chunkSize/size；
- fail-fast：null 数据、窗≤0。

## User Stories

1. 作为批处理作者，10 倍内存数据排序不 OOM——外部底座。
2. 作为审计作者，runCount 显形——IO 轮次可预估。

## Testing Decisions

- 20 组随机 250 值窗 64 vs Arrays.sort 圣像全等+游程数 4；
  游程公式（100/30=4、90/30=3、30/30=1、0/30=0）；单块+
  重复值；fail-fast。

## Out of Scope

- 不做真实 IO/流式（数组模型）；不做堆选路（线性选最小
  ——路数小简洁）。

## Further Notes

- 与 BuddyAllocator（T31）同族不同面：分配器 vs 排序管道。
- 里程碑：T32/50（64%）。
