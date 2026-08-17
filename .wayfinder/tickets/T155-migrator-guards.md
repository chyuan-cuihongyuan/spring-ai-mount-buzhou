---
Type: task
Status: closed
---
## Question

SchemaMigrator 只跑 version > maxApplied：旧构建对上新库静默通过（无「检测到未来版本」拒绝），且版本表无 checksum（已应用脚本事后被改无法发现）：防护语义与既有 V3 库兼容策略如何定？

## Resolution

AFK 自决：双防护落地——(a) 未来版本拒绝（maxApplied > 本构建最新脚本即 IllegalStateException，
Flyway validateOnMigrate 等价）；(b) 版本表增 checksum 列（脚本 sha256；存量缺列幂等 ALTER 补列、
NULL 行首升回填锚定；已应用脚本与记录不符拒绝迁移）。基线行同写 checksum。MySQL DDL 隐式提交
既有洞显式出界。产 spec 42 §A + impl-126。
