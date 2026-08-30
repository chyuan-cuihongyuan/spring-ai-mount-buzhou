---
Type: task
Status: closed
---
## Question

SessionEventListener 装饰器：出站事件 payload String 值脱敏。

## Resolution

done（2026-08-30）：impl-302；PiiEventRedactor（PiiDetector 内置五型 +
CustomPiiRules 叠加；非 String 原样；无命中零改写；fail-open）。
