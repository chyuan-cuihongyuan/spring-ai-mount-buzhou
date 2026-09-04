---
Type: task
Status: closed
---
## Question

IdleCompactionHousekeeper 选主门 + 两处装配 ObjectProvider 注入 +
README 收口（快照零 diff 预判）。

## Resolution

done（2026-09-04）：impl-364；housekeeper 构造器重载+门+让位三用例
绿；core/memory 两装配点注入（无 bean 零变化）；README 纵深 IV 加行、
覆盖门绿；快照 regenerate 零 diff 确认。
