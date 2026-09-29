# impl 2352 — V 会话 V50 收口对账（spec 8050 / V8101–V8102 / V50）

纵切片：V 会话封卷对账——快照补登 +1 + api-surface/CONTEXT 同步 + 组合定向 verify 绿。

- 验证：core 全量（除 Radix）BUILD SUCCESS + Radix 单测绿 + 三门 reactor 绿（R48 环境豁免口径）。
