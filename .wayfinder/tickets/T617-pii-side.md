---
Type: task
Status: closed
---
## Question

Side 维度（INPUT/OUTPUT/UNSPECIFIED）+ 双钩接线 + 分侧查询。

## Resolution

done（2026-09-01）：impl-336；PiiHitStats.Side + 双参重载（单参 UNSPECIFIED
兼容）+ topBySide/countOf(name,side)；输入钩→INPUT、输出钩→OUTPUT。
