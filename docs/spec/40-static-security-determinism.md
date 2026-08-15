# Spec 40 — 静态安全与运行时确定性（effort #9）

> effort #9 主线 spec。§A：spill 落盘静态加密（T151）；§B：会话单飞闸（T152）。
> 延续 spec 02（spill）/ spec 13 增长护栏 / spec 07（hooks）既有语义。

## §A spill 落盘静态加密（T151 / impl-122）

### Problem Statement

超大工具返回自动落盘（spill）的 `.spill` 数据文件是纯明文（spec 02 留位「静态加密未定」）；
临时目录/共享盘上的落盘内容对任何有文件系统读权限者完全可见——生产环境的合规硬缺口。

### Solution

提供可配的 spill 落盘加密面：配置 `buzhou.spill.encryption-key`（Base64 编码 32 字节 AES 密钥）
即开启；缺省不加密（零行为变化）。加密后磁盘上只有密文，读回（load/read_range/完整性复验）对
调用方透明。

### User Stories

1. As a 平台安全负责人, I want spill 落盘内容静态加密, so that 磁盘备份/快照泄露不暴露会话敏感数据。
2. As a 运维工程师, I want 用环境变量注入加密密钥, so that 密钥不进代码库与镜像。
3. As a 运维工程师, I want 升级开启加密后仍能读旧明文 spill 文件, so that 存量证据不因开启加密而失效。
4. As a 运维工程师, I want 密钥错配时读侧快速失败并给出明确语义, so that 静默解密错误不会污染上下文。
5. As a 开发者, I want 不配置密钥时行为与既往完全一致, so that 加密面是纯增量能力。
6. As a 开发者, I want 非法密钥（长度/编码错）启动即拒, so that 配置错误不延后到运行期。
7. As a 审计员, I want 落盘完整性仍可复验（sha256 锚点）, so that 加密不破坏既有完整性语义。

### Implementation Decisions

- 新公共类型 `SpillCipher`（buzhou-spill）：`fromBase64Key(String)` 工厂（32 字节强校验）；
  `encrypt(String)`/`decryptIfEncrypted(String)`；wire 格式 = 首行魔法 `BUZHOU-ENC-V1` + Base64(随机
  12B IV ‖ AES-256-GCM 密文)。每次加密随机 IV（同明文两次落盘密文不同）。
- `DiskSpillStore` 增三参构造（rootDir, quota, cipher；cipher 可空=直通）；仅 `.spill` 数据文件加密，
  meta 文件保持明文 JSON（含明文 sha256 完整性锚——摘要不泄露内容语义，沿用）。
- 读侧魔法前缀探测：无魔法 = 旧明文直通（向后兼容）；有魔法 = 解密；GCM 验签失败快速失败。
- 装配面：`SpillProperties` 增 `encryptionKey`（旧参兼容构造保留）；`SpillModule` 增带 cipher 构造；
  auto-config 按属性非空即 `SpillCipher.fromBase64Key`（非法值 fail-fast）。
- 纯 JDK `javax.crypto`，零新依赖（10K★ 政策：语义借鉴 Dify 凭据 AES-GCM 加密，不引依赖）。

### Testing Decisions

- 好测试只测外部行为：经 `DiskSpillStore` 公开面（store/load/readRange/verifyIntegrity）断言。
- 模块：buzhou-spill 单测（SpillStoreTest 既有风格 + 新 SpillEncryptionTest）。
- 用例：密文落盘（磁盘文件不含明文且含魔法行）/ 加密往返 load 原文 / 旧明文文件兼容读 /
  密钥错配快速失败 / 默认关零变化 / 非法密钥构造拒绝。

### Out of Scope

- StateEntry/JDBC/Redis at-rest 加密（部署层盘加密 + TLS 属运维职责，runbook 指引）。
- 密钥轮换/多版本密钥环（审计链 SigningKeyRing 域，见 §后续轮）。

## §B 会话单飞闸（T152 / impl-123）

### Problem Statement

同一会话上并发发起多个轮次（chat/stream/chatForEntity）既往属「未定义使用」——仅文档声明，
框架不拦截：并发轮次会交叉读写历史与预算计数，产生不可解释的行为；调用方误用时得到的是
静默的数据混乱而非明确错误。

### Solution

框架层提供 per-session 单飞闸：同会话在途轮次未终结时，第二个轮次入口调用立即以结构化错误
`TURN_IN_FLIGHT`（NON_RETRYABLE）拒绝；轮次终结（正常完成、异常收尾、流式 doFinally）即释放。
语义由「未定义」升级为「确定拒绝」。

### User Stories

1. As a 应用开发者, I want 同会话并发轮次被确定拒绝并拿到结构化错误码, so that 误用立刻暴露而非静默数据混乱。
2. As a 应用开发者, I want 失败收尾的轮次也释放闸, so that 一次模型故障不会卡死整个会话。
3. As a 应用开发者, I want 流式轮次终结（含取消）后闸释放, so that 流式与同步入口语义一致。
4. As a SRE, I want 错误码带 RetryCategory 分诊, so that 告警策略可自动化（NON_RETRYABLE 不重试）。

### Implementation Decisions

- `DefaultAgentSession` 在途计数 AtomicInteger 由「只计不拦」升级为 CAS 0→1 占位闸（chat /
  chatForEntity / stream 三入口同一闸）；占位失败抛 `BuzhouException(ErrorCode.TURN_IN_FLIGHT)`，
  消息携带 sessionId 与处置指引（等待在途终结或 spawn 独立会话）。
- `ErrorCode` 新增 `TURN_IN_FLIGHT`（NON_RETRYABLE——在途轮次存在期间重试必然再拒）。
- 默认开启、无关闭开关：既往语义是未定义行为，不存在合法的并发依赖面（10K★ 政策：OpenHands
  每会话事件串行化同款取向——确定性优先于并发宽容）。
- 诚实边界：`stream()` 返回的 Flux 若从不订阅，闸位如同既往在途计数一样不释放（冷流一次性
  订阅契约，javadoc 显式化）。
- 跨进程并发仍由会话租约门（SESSION_ALREADY_ACTIVE / LEASE_LOST）承担——单飞闸只管进程内。

### Testing Decisions

- 只测外部行为：core 级（FakeChatModel/阻塞工具）——在途期间第二入口确定拒绝（含 stream 入口）；
  完成后闸释放可续聊；模型异常收尾同样释放闸。
- Prior art：DefaultAgentRuntimeShutdownTest（同款 harness/latch 风格）。

### Out of Scope

- 排队/合并并发轮次（排队引入隐式时序，与确定性目标相反）。
- 跨进程单飞（租约域）。
