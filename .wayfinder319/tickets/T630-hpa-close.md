---
Type: task
Status: closed
---
## Question

回归与收口：驱动真实舱的外部行为用例（拒绝升倍/回零回落/钳制/多 agent
各自建议）；装配测试（舱开 + scale-up-threshold 配 → 装配；threshold 未配
或舱未开 → 不装配）；README 纵深 IV 加行（spec 319）；PROGRESS 台账。

## Resolution

done（2026-09-02）：impl-342；装配五用例绿（复合 Condition——NullBean 定义
类型仍参与类型匹配，doesNotHaveBean 不认账，改 312 同法 Binder 预绑判定）。
