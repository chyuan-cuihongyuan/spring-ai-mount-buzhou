# impl 1464 — CredentialStrengthMeter 凭据强度计（R64 = effort #1863 / spec 1863 / T2927-T2928）

**What**：`CredentialStrengthMeter`（buzhou-guard/secret 静态纯函数）——
charClasses 四类计数 + band 双条件阶梯（3 类×16 长 STRONG/3 类×12 长
FAIR/否则 WEAK，常量显式）；空白 fail-fast。

**Why**：密码强度计惯例（NIST 长度优先+类别多样性）思想——弱钥（短、
单一字符类）静默落盘是被爆破根因；与 SecretScanner 互补：扫描器防泄漏
（文本检测）、强度计防弱钥（已知凭据评分）。

**Verify**：`CredentialStrengthMeterTest` 4 用例全绿。

**Status**：done（2026-09-16）
