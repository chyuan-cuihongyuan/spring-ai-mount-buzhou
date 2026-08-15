package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * PKCS#8 PEM 文件密钥加载器（impl-39 / spec 13 §T64）：配置指路径、不进代码——
 *
 * <pre>
 * buzhou.guard.audit.signing.keys[0].version=1
 * buzhou.guard.audit.signing.keys[0].private-key-path=/etc/buzhou/audit/v1.pem
 * </pre>
 *
 * <p>私钥须为 PKCS#8（{@code -----BEGIN PRIVATE KEY-----}，{@code openssl pkcs8 -topk8}
 * 转换）；公钥可省略——openssl 生成的 EC PKCS#8 材料内嵌公钥点（ECPrivateKey ASN.1 的
 * [1] BIT STRING），本类经 ~60 行 ASN.1 提取恢复；显式给出时须为 X.509
 * {@code -----BEGIN PUBLIC KEY-----}。每个文件一个密钥（K8s Secret 单文件挂载惯例）。
 */
public final class PemFileKeyProvider implements SigningKeyProvider {

    /** version → 密钥文件路径（公钥路径可空 = 从私钥材料恢复）。 */
    private final List<Entry> entries;

    public record Entry(int version, Path privateKeyPath, Path publicKeyPath) {
        public Entry(int version, Path privateKeyPath) {
            this(version, privateKeyPath, null);
        }
    }

    public PemFileKeyProvider(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * spec 41 §A / T153 / impl-124：目录扫描构建——按 {@code v<version>.pem}（私钥）+
     * {@code v<version>.pub.pem}（公钥，可缺省）约定命名发现全部版本（与
     * {@link PemFileKeyPersister} 同一约定闭环：运行期轮换写入的新钥重启后自动入环）。
     * 目录不存在/无匹配文件返回空 provider（纯哈希链降级）。
     */
    public static PemFileKeyProvider scanDirectory(Path dir) {
        List<Entry> entries = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                for (Path file : stream.sorted().toList()) {
                    String name = file.getFileName().toString();
                    if (!name.startsWith("v") || !name.endsWith(".pem") || name.endsWith(".pub.pem")) {
                        continue;
                    }
                    try {
                        int version = Integer.parseInt(name.substring(1, name.length() - 4));
                        entries.add(new Entry(version, file,
                                Files.exists(file.resolveSibling("v" + version + ".pub.pem"))
                                        ? file.resolveSibling("v" + version + ".pub.pem") : null));
                    } catch (NumberFormatException ignored) {
                        // 非 v<version>.pem 命名的文件不属密钥环（目录共享场景宽容跳过）
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("审计签名密钥目录扫描失败：" + dir, e);
            }
        }
        return new PemFileKeyProvider(entries);
    }

    @Override
    public List<VersionedSigningKey> load() {
        List<VersionedSigningKey> keys = new ArrayList<>();
        for (Entry entry : entries) {
            PrivateKey privateKey = parsePrivateKey(readPem(entry.privateKeyPath()));
            PublicKey publicKey = entry.publicKeyPath() == null
                    ? recoverPublicKey(privateKey)
                    : parsePublicKey(readPem(entry.publicKeyPath()));
            keys.add(new VersionedSigningKey(entry.version(), privateKey, publicKey));
        }
        return keys;
    }

    private static byte[] readPem(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.UTF_8);
            String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        } catch (IOException e) {
            throw new IllegalStateException("审计签名密钥文件不可读：" + path, e);
        }
    }

    private static PrivateKey parsePrivateKey(byte[] der) {
        try {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "审计签名私钥须为 PKCS#8 PEM（BEGIN PRIVATE KEY；openssl pkcs8 -topk8 转换）", e);
        }
    }

    private static PublicKey parsePublicKey(byte[] der) {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("审计签名公钥须为 X.509 PEM（BEGIN PUBLIC KEY）", e);
        }
    }

    /**
     * 从 EC 私钥恢复公钥（两路）：
     * <ol>
     *   <li>PKCS#8 材料内嵌公钥点（openssl 默认形态）→ 直接取点；</li>
     *   <li>JDK SunEC 编码不内嵌 → ECDH 技巧：{@code KeyAgreement(d, G)} 的共享密钥即
     *       {@code d·G} 的 x 坐标，曲线方程解出 y（NIST 素域 p ≡ 3 (mod 4) 平方根直接取幂），
     *       ±y 两候选经「私钥签名 → 候选公钥验签」恰一通过消歧。</li>
     * </ol>
     */
    private static PublicKey recoverPublicKey(PrivateKey privateKey) {
        if (!(privateKey instanceof java.security.interfaces.ECPrivateKey ecPrivate)) {
            throw new IllegalStateException("审计签名私钥须为 EC（收到 "
                    + privateKey.getAlgorithm() + "）；请配置 public-key-path");
        }
        byte[] point = extractEmbeddedPublicKey(privateKey.getEncoded());
        if (point != null) {
            try {
                return KeyFactory.getInstance("EC")
                        .generatePublic(new ECPublicKeySpec(parsePoint(point, ecPrivate),
                                ecPrivate.getParams()));
            } catch (Exception e) {
                throw new IllegalStateException("从私钥材料恢复公钥失败；请配置 public-key-path", e);
            }
        }
        return recoverViaEcdh(ecPrivate);
    }

    /** ECDH(d, G) → x → 曲线方程解 y → 签名/验签消歧（纯 JDK API，无第三方依赖）。 */
    private static PublicKey recoverViaEcdh(java.security.interfaces.ECPrivateKey ecPrivate) {
        try {
            KeyFactory factory = KeyFactory.getInstance("EC");
            java.security.spec.ECParameterSpec params = ecPrivate.getParams();
            PublicKey basePoint = factory.generatePublic(
                    new ECPublicKeySpec(params.getGenerator(), params));
            javax.crypto.KeyAgreement agreement = javax.crypto.KeyAgreement.getInstance("ECDH");
            agreement.init(ecPrivate);
            agreement.doPhase(basePoint, true);
            java.math.BigInteger x = new java.math.BigInteger(1, agreement.generateSecret());

            java.security.spec.EllipticCurve curve = params.getCurve();
            java.math.BigInteger p = ((java.security.spec.ECFieldFp) curve.getField()).getP();
            java.math.BigInteger rhs = x.modPow(java.math.BigInteger.valueOf(3), p)
                    .add(curve.getA().multiply(x)).add(curve.getB()).mod(p);
            java.math.BigInteger y = rhs.modPow(p.add(java.math.BigInteger.ONE)
                    .shiftRight(2), p);
            if (!y.multiply(y).mod(p).equals(rhs)) {
                throw new IllegalStateException("ECDH 恢复的点不在曲线上（参数异常）");
            }
            PublicKey positive = factory.generatePublic(
                    new ECPublicKeySpec(new java.security.spec.ECPoint(x, y), params));
            if (verifyRoundtrip(ecPrivate, positive)) {
                return positive;
            }
            PublicKey negative = factory.generatePublic(
                    new ECPublicKeySpec(new java.security.spec.ECPoint(x, p.subtract(y)), params));
            if (verifyRoundtrip(ecPrivate, negative)) {
                return negative;
            }
            throw new IllegalStateException("±y 候选均无法验签（ECDH 公钥恢复失败）");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("从私钥材料恢复公钥失败；请配置 public-key-path", e);
        }
    }

    /** 私钥签一条固定消息，候选公钥验签（消歧 oracle）。 */
    private static boolean verifyRoundtrip(java.security.interfaces.ECPrivateKey privateKey,
            PublicKey candidate) {
        byte[] message = "buzhou-audit-keyring-public-recovery".getBytes(StandardCharsets.UTF_8);
        try {
            java.security.Signature signer = java.security.Signature
                    .getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(message);
            byte[] signature = signer.sign();
            java.security.Signature verifier = java.security.Signature
                    .getInstance("SHA256withECDSA");
            verifier.initVerify(candidate);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static java.security.spec.ECPoint parsePoint(byte[] point,
            java.security.interfaces.ECPrivateKey ecPrivate) {
        int byteLen = (ecPrivate.getParams().getCurve().getField().getFieldSize() + 7) / 8;
        if (point.length != 1 + 2 * byteLen || point[0] != 4) {
            throw new IllegalStateException("内嵌公钥点非未压缩形式（0x04||X||Y），无法恢复");
        }
        return new java.security.spec.ECPoint(
                new java.math.BigInteger(1, java.util.Arrays.copyOfRange(point, 1, 1 + byteLen)),
                new java.math.BigInteger(1, java.util.Arrays.copyOfRange(point, 1 + byteLen,
                        point.length)));
    }

    // ---- 最小 ASN.1 TLV 游走（仅本类使用；PKCS#8/ECPrivateKey 结构固定、无变长陷阱）----

    /** PKCS#8{version,algId,inner} → ECPrivateKey{version,key,[0]params,[1]publicKey}。 */
    private static byte[] extractEmbeddedPublicKey(byte[] pkcs8Der) {
        List<Der> outerParts = Der.childrenOf(pkcs8Der);
        // SEQUENCE{ INTEGER version, SEQUENCE algId, OCTET STRING inner, [0]?, [1]? }
        for (int i = 3; i < outerParts.size(); i++) {
            if (outerParts.get(i).tag() == 0xA1) {
                return bitStringBytes(outerParts.get(i));
            }
        }
        for (Der part : outerParts) {
            if (part.tag() == 0x04) {
                for (Der child : Der.childrenOf(part.contents())) {
                    // [1] explicit BIT STRING（openssl 输出形态）
                    if (child.tag() == 0xA1) {
                        return bitStringBytes(child);
                    }
                }
            }
        }
        return null;
    }

    private static byte[] bitStringBytes(Der explicitTag) {
        List<Der> parts = Der.childrenOf(explicitTag.contents());
        if (parts.size() == 1 && parts.getFirst().tag() == 0x03
                && parts.getFirst().contents().length > 1) {
            byte[] content = parts.getFirst().contents();
            if (content[0] != 0) {
                throw new IllegalStateException("公钥 BIT STRING 首字节应为 unused-bits=0");
            }
            return java.util.Arrays.copyOfRange(content, 1, content.length);
        }
        return null;
    }

    /** 单个 DER TLV 节点（自记总长，供顺序游走）。 */
    private record Der(int tag, byte[] contents, int totalLength) {

        static Der parse(byte[] data, int offset) {
            int tag = data[offset] & 0xFF;
            int first = data[offset + 1] & 0xFF;
            int length;
            int header;
            if (first < 0x80) {
                length = first;
                header = 2;
            } else if (first == 0x81) {
                length = data[offset + 2] & 0xFF;
                header = 3;
            } else if (first == 0x82) {
                length = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
                header = 4;
            } else {
                throw new IllegalStateException("不支持的 DER 长度形式：0x"
                        + Integer.toHexString(first));
            }
            byte[] contents = java.util.Arrays.copyOfRange(data, offset + header,
                    Math.min(offset + header + length, data.length));
            return new Der(tag, contents, header + contents.length);
        }

        /** 顺序游走一个 constructed 内容体（SEQUENCE / explicit tag 的负载）。 */
        static List<Der> childrenOf(byte[] contents) {
            List<Der> children = new ArrayList<>();
            int offset = 0;
            while (offset < contents.length) {
                Der child = Der.parse(contents, offset);
                children.add(child);
                offset += child.totalLength();
            }
            return children;
        }
    }
}
