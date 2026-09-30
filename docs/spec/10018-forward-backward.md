# Spec 10018 — ForwardBackward（effort #10018，X19）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10037–X10038，impl 2421）。
> 借鉴：Forward–Backward（Rabiner 1989——htk/hts 语音同源）

## Problem Statement

HMM 全路径似然与期望统计基座——Viterbi 只给最优路径，软对齐需 α/β 全路径概率。

## Solution

ForwardBackward（core/eval，静态纯函数面）：run(a,b,pi,obs)——缩放 α/β 联合递推防下溢，ln 似然=∑ln c_t；维数/观测越域/负概率/零概率序列 fail-fast。

## Testing Decisions

伞世界手锚 P=0.209 与全路径穷举互证（30 随机模型逐一对拍）+β 一致性+500 步长序列缩放稳定+fail-fast 四面。

## Out of Scope

不做 BaumWelch 重估（下轮异面）；不做 Viterbi 解码头（已占）；不做连续观测密度。

## Further Notes

与 Viterbi（已占）同域不同面：全路径似然 vs 最优单路径；Wave 4 序列组学与 HMM 族首件。
