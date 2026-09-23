# Spec 4042 — 三方合并（effort #4042，R43）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6085–T6086，impl 2143）。
> 借鉴：git merge-file / diff3 三方合并。

## Problem Statement

并发编辑同一文档的病：后写全量覆盖（先写者编辑丢失）或
逐行锁（吞吐崩）——**以基准为锚的自动合并 + 冲突显形面**
缺失。

## Solution

`ThreeWayMerge`（core/policy）：

- LCS 行级 diff（base↔ours、base↔theirs，确定性走格——同长
  优先删）产出双方改动 hunk（base 区间 + 替换行）；
- 单侧改动 → 自动取该侧；双侧改动**相同内容** → 自动归一
 （git 同款：同改不算冲突）；双侧改动**相接或重叠且不同** →
  冲突（保守口径：相邻也算冲突——次序不可辨）；
- 冲突块以 git 风格标记物化（`<<<<<<< ours` / `=======` /
  `>>>>>>> theirs`）+ 结构化 `Conflict(oursLines, theirsLines)`
  双读数；`Outcome(lines, conflicts)`；
- fail-fast：null 入参。

## User Stories

1. 作为会话状态编辑作者，双端增量写入自动归一、真冲突显形
   不静默覆盖。
2. 作为审计作者，同输入同合并结果（确定性可回放）。

## Testing Decisions

- 单侧改取侧 / 双侧不同区自动合 / 双侧同改归一（0 冲突）/
  同区异改冲突（结构化两侧 + 标记物化）/ 双侧同位追加冲突
 （相邻保守口径）；确定性回放。

## Out of Scope

- 不做合并策略选项（patience/histogram diff）；不做移动检测
 （rename/reorder）；不做字符级合并。

## Further Notes

- 与 JSON Patch（RFC 6902）同族不同面：操作重放 vs 状态
  对账合并。Wave 8（图文本治理族）开波。
- 里程碑：43/50。
