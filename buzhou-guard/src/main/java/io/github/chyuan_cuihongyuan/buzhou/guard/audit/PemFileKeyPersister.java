package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.util.Base64;

/**
 * PEM 文件轮换持久化器（spec 41 §A / T153 / impl-124）：轮换新钥写入约定命名的 PEM 文件——
 * {@code v<version>.pem}（PKCS#8 私钥）+ {@code v<version>.pub.pem}（X.509 公钥），原子
 * tmp+move 落盘。与 {@link PemFileKeyProvider#scanDirectory(Path)} 同一命名约定闭环：
 * 运行期轮换写入的 v2/v3… 重启后由目录扫描自动入环（active = 最高版本）。
 *
 * @since 1.0.0
 */
public final class PemFileKeyPersister implements SigningKeyPersister {

    private final Path dir;

    public PemFileKeyPersister(Path dir) {
        this.dir = dir;
    }

    /** 轮换新钥的私钥文件名（与 scanDirectory 约定一致）。 */
    public static Path privateKeyFile(Path dir, int version) {
        return dir.resolve("v" + version + ".pem");
    }

    /** 轮换新钥的公钥文件名（与 scanDirectory 约定一致）。 */
    public static Path publicKeyFile(Path dir, int version) {
        return dir.resolve("v" + version + ".pub.pem");
    }

    @Override
    public void persist(int version, KeyPair keyPair) {
        try {
            Files.createDirectories(dir);
            writeAtomically(privateKeyFile(dir, version),
                    toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
            writeAtomically(publicKeyFile(dir, version),
                    toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        } catch (IOException e) {
            throw new UncheckedIOException("审计签名密钥轮换持久化失败（version=" + version
                    + ", dir=" + dir + "）——轮换中止，active 不变", e);
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** DER → 单行 Base64 体 PEM（K8s Secret 单文件挂载惯例同 PemFileKeyProvider）。 */
    private static String toPem(String label, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
    }
}
