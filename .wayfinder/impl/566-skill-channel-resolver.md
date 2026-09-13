# impl 566 — SkillChannelResolver（effort #813）

## 切片

- `buzhou-skills/src/main/java/.../skill/SkillChannelResolver.java` — Map.copyOf(name→channel→version) 不可变注册表+resolve/channelsOf/names+compareVersions（compareSegment：数字段/prerelease 低于 release/字典序兜底）。
- `buzhou-skills/src/test/java/.../skill/SkillChannelResolverTest.java` — 5 例。

## 口径

- 同通道重复标定在构造期收敛（保留 compareVersions 更高者）——运行期只读零竞争。
- prerelease 判定：数字前缀后带非空后缀即 prerelease（0-beta/1-rc.1 形态）。

## 验证

mvn -pl buzhou-skills -am test -Dtest='SkillChannelResolverTest' → 5/5 绿；快照再生 1 新公共类型。
