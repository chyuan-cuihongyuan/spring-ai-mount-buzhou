# Spec 8047 — PaaCodec（effort #8047，V48）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8095–V8096，impl 2349）。
> 借鉴：Keogh 2001 分段聚合近似思想。

## Problem Statement

时序降维的病：抽稀丢形状——**等宽分段均值代表**（w 段，余数段并入末段）。

## Solution

PaaCodec（core/metrics）：transform(series,w) 返回段均值 double[]+w∈[1,n] 越域 fail-fast+整除/非整除两路+确定性。

## Testing Decisions

整除段均值逐值手锚+非整除余数并入+300 随机与逐段均值圣像全等+重构误差 ≥0 单调于 w；fail-fast。

## Out of Scope

- 不做多序列/带约束比对（成对面明示）。

## Further Notes

- 里程碑：V48/50。
