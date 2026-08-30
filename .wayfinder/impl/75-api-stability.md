# 75 — API 稳定性审计（T100 决策落地）

**What to build:** docs/api-surface.md（14 模块清单）+ internal 审计 + CONTRIBUTING 稳定性政策。

**Blocked by:** 57、60、62、63、64、65、66（全部已 done）。

**Status:** done

## Done

验证：清单脚本生成（404 public 类型 / 14 模块；36 public-in-internal 计数核对）；CONTRIBUTING 政策追加。
落地：`docs/api-surface.md`（模块×类型清单 + starter 零类型 + internal 审计节 + 稳定性政策节）；CONTRIBUTING 增「API 稳定性政策」（语义化版本/废弃≥2 minor/@since 1.0.0 起/internal 与 fromYml map 契约豁免）；javadoc 核查结论=关键接口既往已齐，不追溯补标。