# 69 — redteam F1 数值化 + 场景扩充（T94 决策落地）

**What to build:** redteam/metrics.mjs（plugin 汇总 + 硬门裁决）+ promptfoo plugins 扩充 + nightly workflow 硬门步骤 + baseline.md 数值化。

**Blocked by:** None.

**Status:** done

## Done

验证：metrics.mjs 样例烟测（3 样本 66.7% 拦截率 → 表格正确 + 硬门 exit 1；全绿样例路径经单元参数覆盖）；workflow YAML 步骤接线。
落地：`redteam/metrics.mjs`（dangerous-executed=0 + 拦截率≥95% 双硬门，INTERCEPT_MIN/DANGEROUS_MAX 可调，工件 json+md）；plugins 增 `pii:direct` + `harmful:injury`；redteam-nightly.yml 增「Compute metrics and enforce hard gates」步骤（替代裸 || true 观测——确定性不变量升硬门，随机面仍观测）；baseline.md 增 F1 数值化口径（F1=R，FP 通道=examples 授权闭环 FP=0）与工件清单。