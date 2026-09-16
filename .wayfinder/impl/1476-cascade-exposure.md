# impl 1476 — CascadeExposure 级联失败暴露（R76 = effort #1875 / spec 1875 / T2951-T2952）

**What**：`CascadeExposure`（buzhou-resilience 静态纯函数）——Edge 权重
（share×failureRate）+ Exposure（最脆边/全图损失面/活静风险分诊）；
畸形四型 fail-fast。

**Why**：级联失败分析思想——熔断逐点防护是事后；边权重排序事前回答
「哪里最脆」（主流量压高失败率下游），活/静分诊先掐传导再排埋雷。

**Verify**：`CascadeExposureTest` 4 用例全绿（浮点容差断言修正一次）。

**Status**：done（2026-09-16）
