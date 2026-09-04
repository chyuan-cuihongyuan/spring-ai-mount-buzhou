# Spec 333 — 消息静态信封加密（effort #333）

> wayfinder map：`.wayfinder333/MAP.md`（T657–T658）。C 会话第 34 轮。

## Problem Statement

消息（含内容、推理、工具实参）在存储层全程明文：JDBC 表、Redis 键值、
归档冷层、JSONL 导出。共享 Redis / 托管数据库 / 备份落盘场景下，
存储管理员与备份接触面能读走全部会话内容——敏感面不受进程内密钥
控制。库内没有任何加密机制。

## Solution

应用层信封加密（Vault transit / AWS KMS envelope 思想——密钥不出进程，
存储只见密文）：

- **`EnvelopeCipher`**（core.crypto）：AES-256-GCM；信封格式
  `buzhou:v1:<keyId>:<base64(iv‖ct+tag)>`；keyId = 主钥 SHA-256 前 8 字节
  hex（信封自描述——解密按 keyId 路由）；AAD 绑定消息标识（id+sessionId）
  ——密文剪贴到别的消息/会话认证失败；当前钥加密、当前+前代双钥解密
  （`previous-master-key` 轮换面：写新读旧，平滑换钥）；GCM 认证失败
  上抛（篡改宁可炸不可静默——完整性优先）。
- **`EncryptingMessageStore`**（MessageStore 装饰器）：append 时每条消息
  序列化（手写归一映射——不依赖 jackson-jsr310）+ 加密成载体消息
  （id/sessionId/turnSeq/seqInTurn/createdAt 明文供排序检索路由，role
  以 USER 占位、真值在密文内，内容字段承载信封）；load/findById 解密
  还原；非载体旧数据透传（既有明文库迁移友好——不炸不重复加密）；
  deleteSession 直通。
- **装配**：`buzhou.security.message-encryption.master-key`（Base64 AES
  钥，16/24/32 字节）声明即启用——BeanPostProcessor 捕获 BuzhouStores
  bean 重建（仅换 messageStore 槽，宿主零改动）；`previous-master-key`
  可选（轮换期双钥）。未配 master-key = BPP 不存在，零行为变化。

## User Stories

1. 作为安全负责人，我想消息内容在存储层全程密文，所以 存储管理员/
   备份接触面读不走会话内容。
2. 作为安全负责人，我想密钥留在进程内（yml/env 注入），所以 不引入
   外部密钥服务依赖也能达到静态加密。
3. 作为运维，我想换钥时旧数据仍可读（双钥窗口），所以 轮换不需要
   一次性重加密全库停机。
4. 作为运维，我想新写入永远用新钥，所以 轮换后密文面自然收敛到新钥。
5. 作为开发者，我想密文被剪贴到别的消息/会话时解密失败，所以 篡改
   与错位不会被静默放过。
6. 作为使用者，我想既有明文库升级后照常可读（透传兼容），所以 不用
   先迁移再升级。
7. 作为使用者，我不想配置加密时行为与现状完全一致，所以 升级零风险。
8. 作为审计者，我想信封自描述 keyId，所以 哪代钥加密的密文可离线
   判别、轮换收敛可观测。

## Implementation Decisions

- 序列化：BuzhouMessage → 归一 Map（createdAt→epochMillis、Role→name、
  ToolCallRecord→Map），Jackson databind 序列化；metadata 值经 JSON
  往返归一（文档化的诚实代价）。
- 载体消息 role 占位 USER（真 role 在密文内）；createdAt 保留明文——
  时间排序路由不破。
- BPP 捕获 BuzhouStores（postProcessAfterInitialization 返回重建 record）；
  只换 messageStore 槽，其余五槽原样。
- 密钥校验 fail-fast：非 Base64 / 非 16、24、32 字节启动红（带修法）。

## Testing Decisions

- Cipher：往返 / AAD 换绑失败 / 密文篡改抛 / 双钥轮换（旧信封可解、新写
  新 keyId）/ 幽灵 keyId 拒 / 非法钥参数红。
- Store：append 后底层只见载体（content 为信封、role 占位）/ load 还原
  全字段（含 toolCalls/metadata/createdAt）/ findById / 旧明文透传 /
  重复包装安全（已是信封不再加密）。
- 装配：master-key 声明即 BuzhouStores 被 BPP 换装（messageStore 事实
  类型变化 + 容器内 append/load 往返）/ 未配零变化 / 坏钥启动红。

## Out of Scope

- SummaryStore/SessionStateStore/导出 JSONL 加密（同通道扩散另轮）；
- 外部 KMS/Vault 集成（KeyProvider SPI）；密文内检索；每消息 DEK 链
  （信封分级 KMS 托管——当前单层应用钥已覆盖威胁模型）。

## Further Notes

- 新公共类型（EnvelopeCipher/EncryptingMessageStore/属性类/BPP）随轮
  regenerate API 快照。
- 与 313 PII 分侧互补：313 识别并计数，本轮让存储面读不走。
