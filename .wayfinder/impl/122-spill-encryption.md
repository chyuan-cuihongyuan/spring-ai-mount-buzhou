# impl-122 — spill 落盘静态加密

**What to build:** 配置密钥后 spill 数据文件以 AES-256-GCM 密文落盘，读回透明；缺省零变化。

**Blocked by:** None

**Status:** done

- [x] SpillCipher（fromBase64Key/encrypt/decryptIfEncrypted；魔法前缀 wire 格式）
- [x] DiskSpillStore 三参构造 + store/load 接 cipher；meta 保持明文
- [x] SpillProperties.encryptionKey + 兼容构造；auto-config 接线（fail-fast）
- [x] 测试：密文落盘/加密往返/旧明文兼容/密钥错配/默认关/非法密钥——spill 模块绿

## Done

commit：本轮（spill 122 测试全绿，含新 SpillEncryptionTest 6 用例）。
