# Spec 9018 — Winnow Fingerprint 指纹采样（effort #9018，W19）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9037–W9038，impl 2371）。
> 借鉴：Winnowing（Schleimer 2003——斯坦福 MOSS 抄袭检测同源）

## Problem Statement

全 k-gram 指纹爆炸、随机采样短匹配全漏——
**winnowing**：宽 w 窗取最小哈希，≥k+w−1
匹配段必含公共指纹。

## Solution

WinnowFingerprint（core/metrics，静态工具面）：
fingerprints(data,k,w)→位置序列（升序去重）；
确定性滚动哈希（基 257/模 2³¹−1）。

## Testing Decisions

40 随机文本窗窗必中圣像；局部改动远段稳定；
确定性；w=1 全 gram 退化；fail-fast。

## Out of Scope

不做带位置指纹序列化（位置面）；不做
跨文档指纹比对面（消费方组合）；不做加密哈希。

## Further Notes

与 MinHashSketch 同域不同面。Wave 4 第一件。
