---
id: T1807
title: 低覆盖类批次 1 验证（JaCoCo 复扫 + 模块测试绿）
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1806
created: 2026-09-15
---

## Question

R2 补测后：PolicyGateHook / RecallSearchTool 是否脱离低覆盖档（覆盖率 ≥50% 或剩余 miss <10）？guard / memory 模块测试是否绿？主代码行为是否零变化？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，guard / memory 全量 test + JaCoCo 复扫）：

1. **两靶点脱离低覆盖档**：PolicyGateHook cov 6→116 / mis 11→1（99%）；RecallSearchTool cov 26→230 / mis 2（99%）——均远超 50% 线，残余 miss 为防御分支边缘行（不硬凑）。
2. **模块全量绿**：guard 331 用例 0 失败 0 错误；memory 172 用例 0 失败 0 错误（新增 9 + 12 用例在内）。
3. **主代码零行为变化**：唯一主代码触碰 = T1808 注释更正（零行为变化，独立 commit）；公共 API 面零变化。
4. 补测显形缺陷 1 项（T1808 指标注释 tag 值失真）已单列裁决收口。
