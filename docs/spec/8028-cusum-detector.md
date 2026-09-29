# Spec 8028 — CusumDetector（effort #8028，V29）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8057–V8058，impl 2330）。
> 借鉴：Page 1954 CUSUM（SPC 质量控制）思想。

## Problem Statement

流式变点的病：固定阈值把自然波动当告警——**累计偏移 CUSUM：C+=max(0,C+x−drift)，越 h 即变点**——小漂移累积可见。

## Solution

CusumDetector（core/metrics）：双单边（正/负漂移）累计+feed(x) 返回三态（正常/正越界/负越界）+reset+参数（drift≥0,h>0）越域 fail-fast+确定性纯函数。

## Testing Decisions

手锚（阶跃序列若干步内越界+零漂白噪声不越界）；越界步数单调于 h；fail-fast。

## Out of Scope

- 不做多维/自适应参数面（单参数语义明示）。

## Further Notes

- 里程碑：V29/50。
