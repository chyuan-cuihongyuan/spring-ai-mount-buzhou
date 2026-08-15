# Spec 41 — 审计轮换持久化与运行时时钟（effort #9）

> effort #9 主线 spec。§A：审计密钥轮换持久化 + 链外锚定（T153）；
> §B：时钟注入面（T154）。

## §A 审计密钥轮换持久化与链外锚定（T153 / impl-124）

### Problem Statement

两个真实生产缺口：其一，审计签名密钥运行期轮换（rotate）只在内存切换——新钥不落盘，重启后
按 PEM 文件重建密钥环时轮换钥不在环内，轮换期间签的记录全部变「签名不可验」断链；其二，链头
信任根是常量（SHA-256("")）且无外部锚定——对审计存储有写权限的攻击者可整链重写重算，或直接
删尾记录（链内部仍自洽），纯内部一致性校验检测不到。

### Solution

轮换「写而后切」：rotate 先经持久化钩子把新钥落盘（PEM 文件约定命名），落盘失败则轮换整体
失败、active 不变；启动期密钥目录扫描按约定命名发现全部版本（含轮换写入的），重启自动入环。
链外锚定：校验报告给出重放推得的链头哈希，运维在链外（保险库/异地）保存期望锚点；校验可带
锚点比对——链内部一致但链头与锚点不符即判删尾/重写。

### User Stories

1. As a 安全运维, I want 运行期轮换的密钥自动落盘, so that 重启后新钥仍在环内、轮换期签名不断链。
2. As a 安全运维, I want 轮换落盘失败时 active 不变, so that 不会出现「已切钥但没保存」的不可恢复窗口。
3. As a 审计员, I want 校验报告给出当前链头哈希, so that 我可以把它存到链外作为信任锚。
4. As a 审计员, I want 校验时提供锚点比对, so that 删尾或整链重写可被检测。
5. As a 应用开发者, I want 不配置 key-dir 时行为与既往一致, so that 升级零影响。

### Implementation Decisions

- `SigningKeyPersister` 接口 + `PemFileKeyPersister`（`v<version>.pem` PKCS#8 私钥 +
  `v<version>.pub.pem` X.509 公钥，tmp+move 原子落盘）。
- `SigningKeyRing` 增带 persister 构造；`rotate()` 写而后切（persister 异常上抛、active 不变）。
- `PemFileKeyProvider.scanDirectory(dir)`：目录扫描发现 `v<version>.pem` 全部版本。
- `GuardAuditConfig` 增 `signing.key-dir`（与显式 keys 列表合并）；auto-config 在 key-dir 非空时
  同时挂扫描与持久化器。
- `VerificationReport` 增 `headHash`（重放推得链头；空链 null）与 `anchorMatched`（null=未提供）；
  `AuditChainVerifier.verify(records, ring, expectedHeadAnchor)` 三参重载；`anchored()` =
  intact 且锚点一致。旧 5 参报告构造/2 参 verify 兼容保留。
- 诚实边界：锚点自身的安全取决于链外保存通道；本机制提供的是「检测面」而非「防写面」。

### Testing Decisions

- 只测外部行为（guard 单测，Prior art：AuditChainVerifierTest/SigningKeyRingTest）：
  轮换落盘 + 重启目录扫描再入环 + 轮换期记录可验；持久化失败轮换中止 active 不变；
  锚点一致通过 / 删尾 anchorMatched=false / 未提供锚点 null。

### Out of Scope

- KMS/Vault 托管密钥（persister 接口已留扩展点，集成属部署域）。
- 定时自动轮换（运维动作，runbook 记步骤）。

## §B 时钟注入面（T154 / impl-125）

### Problem Statement

熔断冷却、半开超时、配额 UTC 日窗的时间行为全部依赖系统时钟（JDK 直接调用）——测试只能
Thread.sleep 真实等待冷却/翻日：慢、flake（计时断言时序敏感）、且长冷却窗口（如退避封顶 8×）
根本不可测。时间不可控 = 时间行为不可回归。

### Solution

时间敏感组件提供可注入 Clock（java.time.Clock，构造器可选参，缺省 systemUTC 与既往一致）：
熔断器（ModelCircuitBreaker）与日配额钩子（SessionQuotaHook）先行。测试以可推进时钟替身驱动
状态迁移，零真实等待。

### User Stories

1. As a 框架开发者, I want 熔断冷却/半开迁移可由注入时钟驱动, so that 状态机时间行为测试零等待、不 flake。
2. As a 框架开发者, I want 配额 UTC 日窗翻日可由注入时钟驱动, so that 窗口重置语义可确定性回归。
3. As a 应用开发者, I want 不注入时钟时行为与既往完全一致, so that 升级零影响。

### Implementation Decisions

- `ModelCircuitBreaker` 增三参构造（config, stats, clock）；内部全部 `Instant.now()` →
  `Instant.now(clock)`（跳闸时刻/半开进入/事件时间戳/冷却剩余计算六处）。契约：clock 须 UTC 基。
- `SessionQuotaHook` 增三参构造；`todayKey()` 由静态 `LocalDate.now(UTC)` 改实例 `LocalDate.now(clock)`
  （todayKey 随之转实例方法）。既有两参构造保留（缺省 systemUTC）。
- 诚实边界：ModelRateLimiter（System.nanoTime 单调域）与 ResilienceAdvisor 退避 sleep 不在本片
  注入（单调钟与墙钟语义不同，测试价值/风险比低）；WebhookOutbox.due(Instant,…) 本就是参数化
  时间面，不重复注入。

### Testing Decisions

- 只测外部行为（resilience 单测）：可推进 MutableClock 驱动——三失败样本 OPEN 后推进 61s 即半开放行
  （冷却 60s 配置，零 sleep）；turnsPerDay=1 同日第二轮拦截、推进跨 UTC 午夜再放行。

### Out of Scope

- 全仓统一 Clock 装配 bean（组件各自可选参即可，避免过度装配面）。
