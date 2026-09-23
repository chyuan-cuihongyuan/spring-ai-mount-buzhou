# Spec 5012 — DoubleWrite 双写缓冲（effort #5012，S13）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6125–T6126，impl 2163）。
> 借鉴：InnoDB Doublewrite Buffer（共享缓冲先落、崩溃可恢复）。

## Problem Statement

页级写崩溃一致性的病：就地更新（写到一半崩溃——页撕裂
半新半旧）或全量重做日志（恢复代价大）——**共享暂存
先落 + 崩溃恢复源面**缺失。

## Solution

`DoubleWriteBuffer`（core/recovery）：

- `stage(pageId, data)`：页先写入共享暂存缓冲（同页覆盖——
  最新版胜）；缓冲满 → **自动整体落盘**（清空缓冲 +
  flushCount++）再装入——容量守恒；
- `flush()`：手动整体落盘（返回并清空暂存页）；
- `recoverable()`：崩溃恢复视图——暂存中各页最新版
 （就地更新撕裂时以此为恢复源）；
- 读数：pendingCount/flushCount；fail-fast：pageSlots≤0、
  空/null data。

## User Stories

1. 作为页存储作者，写撕裂有恢复源——半新半旧页可复原。
2. 作为审计作者，同 staging 序列同恢复视图（确定性可回放）。

## Testing Decisions

- stage→recoverable 显影；同页覆盖最新版胜；容量满自动
  落盘（flushCount 递增、缓冲守恒）；flush 清队返回页集；
  空 data/负容量 fail-fast；确定性回放。

## Out of Scope

- 不做真实 IO/校验和（本件是暂存语义面）；不做压缩页；
- 不做双写文件布局（InnoDB .dblwr 格式）。

## Further Notes

- 与 HintedHandoff 同族不同面：投递暂代 vs 崩溃恢复暂存。
  Wave 3 第二件。
- 里程碑：S13/50（26%）。
