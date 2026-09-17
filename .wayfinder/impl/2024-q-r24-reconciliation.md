# impl 2024 — Q 会话 R24 对账轮（spec 3023 / T5047–T5048 / R24）

纵切片：快照 1070→1075（+5 全 Q 系 Wave 4）+ api-surface.md 五行 +
CONTEXT 969→974 + 全仓 verify 三门绿 + push。

- 验证：全仓 mvn verify 16 模块 BUILD SUCCESS（rc 门禁判定）。
- **摇摆观测入档**：首跑抓 1 既有摇摆测试
  （buzhou-resilience CanaryPathEndToEndTest#
  canarySelectedEventCarriesModelAndSession——注释称「权重全给备
  模型确定性选中 secondary」却偶发选 primary；单模块跑绿+全仓复跑
  绿，与 Q 系改动无涉，Wave 4 只碰 core+docs）——P 系 R48 摇摆
  测试先例同款处置：记录不代修（resilience 域归其会话；若再现
  三次以上升级为票）。
