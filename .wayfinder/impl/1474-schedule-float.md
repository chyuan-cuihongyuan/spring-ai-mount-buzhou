# impl 1474 — ScheduleFloat 调度松弛量（R74 = effort #1873 / spec 1873 / T2947-T2948）

**What**：`ScheduleFloat`（core/exec 静态纯函数）——floats 双向 DP（正向
ES + 反向到汇距 → float=LS−ES）→ 逐任务 TaskFloat；环/端点/重复
fail-fast。

**Why**：CPM float/slack 思想——关键路径给了总下界，松弛量给逐任务
缓冲量：谁能让路、让多久，并行削峰的数学依据；float 分布=编排刚性度。

**Verify**：`ScheduleFloatTest` 3 用例全绿（首跑红为心算期望误——R54
入档病理第五次实证）。

**Status**：done（2026-09-16）
