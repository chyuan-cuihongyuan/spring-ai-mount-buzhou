# effort #715 — PII 格式保形掩码

- 会话：G 会话 700 系第 16 轮 ｜ spec [715](../../../docs/spec/715-format-preserving-mask.md) ｜ 票 [T1030](../tickets/T1030-fp-mask.md)/[T1031](../tickets/T1031-fp-mask-verify.md) ｜ impl615
- 借鉴：Presidio（≈4K，微软；同思想族 FPE 标准 NIST FF1）format-preserving 思想——脱敏后保形可解析（本面为结构化掩码简化版，非密码学 FPE）

## 勘察（排重）

- PiiDetector.redact = `[PII:TYPE]` 占位符——下游解析器（日志平台/表单回显）无法再读；507 vault 管「回显授权」不管「掩码形态」。
- grep mask/掩码在 guard/pii：零实现。

## 决定

`FormatPreservingMasker`（guard/pii 静态原语）：`maskPhone`（138****5678 保 3+4）/`maskIdCard`（保 4+2）/`maskEmail`（保首字符+@域名：a***@example.com）/`maskIp`（保前两段）/通用 `mask(text, keepHead, keepTail)`（保长中段打 *）；各类型带形状校验 `isValid*`（校验失败 mask 返回等长全 *——fail-closed 不抛）。纯函数零行为面（宿主展示层自选挂接）。

## 测试

手机/身份证/邮箱/IP 四型掩码形态精确/形状不符全星降级/保长断言/通用掩码边界（keep ≥ 长度）。

## 诚实边界

非密码学 FPE（同输入同掩码——可被形状推断；强去标识归 507 vault/加密）；CN 手机/身份证口径（其余地域宿主用通用掩码自拼）。
