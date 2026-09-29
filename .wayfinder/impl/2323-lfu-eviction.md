# impl 2323 — V 会话 V22 LfuEviction（spec 8021 / V8043–V8044 / V22）

纵切片：LfuEviction（core/cache）——频次桶+最低频先入桶放逐面。

- 验证：`mvn -pl buzhou-core test -Dtest='LfuEvictionTest'` 三测全绿。
