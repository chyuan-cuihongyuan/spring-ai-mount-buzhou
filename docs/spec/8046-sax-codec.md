# Spec 8046 — SaxCodec（effort #8046，V47）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8093–V8094，impl 2348）。
> 借鉴：Lin 2003 SAX 符号聚合近似思想（Keogh 族）。

## Problem Statement

时序相似检索的病：原始浮点序列距离贵——**z 归一+PAA 分段+高斯分位符号化成词**。

## Solution

SaxCodec（core/metrics）：of(wordSize,alphabetSize=3 固定高斯断点)+transform 返回符号串+长度/字母表域 fail-fast+确定性（z 归一零方差诚实全首符）。

## Testing Decisions

手锚（上升序列→全同符号+构造趋势词差分）+300 随机符号域全等+确定性；fail-fast。

## Out of Scope

- 不做多序列/带约束比对（成对面明示）。

## Further Notes

- 里程碑：V47/50。
