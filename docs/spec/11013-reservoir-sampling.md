# Spec 11013 — ReservoirSampling 水塘抽样（effort #11013，Y14）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11027–Y11028，impl 2466）。
> 借鉴：Vitter 1985 Algorithm R 思想——Spark/Kafka 同源

## Problem Statement

未知总量流的均匀 k 抽样——单遍等概率水塘面。

## Solution

ReservoirSampling（core/metrics）：sample(int,int,long)——前 k 直接入塘；i≥k：j=Random(i+1) 命中 [0,k) 则替换；输出升序（确定性契约面）；Random(seed) 可复算。

## Testing Decisions

均匀覆盖圣像（1000 次采样×n=100、k=10：各元素入样率 0.1±0.03）+升序去重+k=n 全集+同种子同序列/异种子异序列+fail-fast 四面。

## Out of Scope

不做分布式水塘（变体另立）；不做加权水塘（A-Res 另立）；不做索引外负载荷泛型面。

## Further Notes

流式均匀抽样的锚定原语；Wave 3 稳健统计与校准族首件。
