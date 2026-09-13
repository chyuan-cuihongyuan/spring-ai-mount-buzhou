# effort #826 — 注入检测分级策略

- 会话：H 会话 800 系第 27 轮 ｜ spec [826](../../../docs/spec/826-injection-paranoia-policy.md) ｜ 票 [T1153](../tickets/T1153-injection-paranoia-policy.md)/[T1154](../tickets/T1154-injection-paranoia-policy-verify.md) ｜ impl579
- 借鉴：ModSecurity paranoia levels（OWASP ModSecurity ≈9K；WAF 敏感度分级通行思想）

## 勘察（排重）

- InjectionClassifier（接口）：Verdict 连续分数——无分级裁决。
- ContentModerationHook：内容审核 hook——分级阈值策略缺位。
- grep -i `paranoia|sensitivity`：无命中。

## 决定

`InjectionParanoiaPolicy`（guard.classifier，纯函数）：Level{L1..L4} 标准阈值表（0.95/0.85/0.70/0.50 类常量可查）——decide(verdict, level)→BLOCK（≥阈值）/LOG（阈值下方 0.10 观察带）/ALLOW；分数越界截断 [0,1]；边界 ≥ 含等号；null fail-fast。纯裁决映射——不改 classifier 行为（采用归调用方）。

## 测试

阈值表四档精确/同分数跨四级三态分化（0.72：ALLOW/ALLOW/BLOCK/BLOCK+L3 观察带 0.65 LOG——首跑预期笔误修正：0.72≥0.70 应 BLOCK）/观察带三界 0.88/0.80/0.70/边界相等 BLOCK/越界截断三例+L4 LOG 带下界/null fail-fast——6 例绿。

## 诚实边界

标准档固定（阈值不可配——ModSecurity 通行口径；定制归调用方自实现映射）；LOG 态语义=记录不拦（征询面归 hook）；分级只影响本裁决不扩大规则集（与 ModSecurity 规则激活差异是口径简化——诚实入档）。
