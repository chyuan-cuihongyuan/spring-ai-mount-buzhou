package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 125 §B / T451：租户隔离沙箱红队——跨租户遍历（相对 {@code ../} 与绝对
 * 他租户路径）一律 SandboxViolation；租户 id 白名单 fail-fast（大写/分隔符/
 * {@code ..}/超长/空白）；本租户读写解析都落在 {@code tenants/<tenant>} 下。
 * 借鉴：Milvus partition-key / OS chroot-per-tenant。
 */
class TenantSandboxTest {

    @Test
    void crossTenantTraversalRejectedBothRelativeAndAbsolute(@TempDir Path root) {
        Path alphaHome = root.resolve("tenants").resolve("alpha");
        FileSandbox alpha = FileSandbox.forTenant(root, "alpha");

        // 相对路径穿越：../tenants/beta/secret
        assertThatThrownBy(() -> alpha.resolve("../tenants/beta/secret"))
                .isInstanceOf(SandboxViolationException.class);
        // 绝对他租户路径
        assertThatThrownBy(() -> alpha.resolve(betaFile(root)))
                .isInstanceOf(SandboxViolationException.class);
        // 写面同样拒绝
        assertThatThrownBy(() -> alpha.resolveForWrite("../tenants/beta/attack"))
                .isInstanceOf(SandboxViolationException.class);

        // 本租户内合法路径：落在 tenants/alpha 下
        assertThat(alpha.root().toString()).endsWith("tenants/alpha");
        assertThat(alpha.resolve("notes/2026.txt").startsWith(alpha.root())).isTrue();
        assertThat(alpha.resolve("notes.txt").startsWith(alpha.root())).isTrue();
    }

    @Test
    void tenantIdWhitelistFailFast() {
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), "Alpha"))
                .isInstanceOf(IllegalArgumentException.class); // 大写
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), "a/b"))
                .isInstanceOf(IllegalArgumentException.class); // 分隔符
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), ".."))
                .isInstanceOf(IllegalArgumentException.class); // 穿越
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), "a b"))
                .isInstanceOf(IllegalArgumentException.class); // 空白
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), "x".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class); // 超长
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), ""))
                .isInstanceOf(IllegalArgumentException.class); // 空
        assertThatThrownBy(() -> FileSandbox.forTenant(Path.of("."), "-lead"))
                .isInstanceOf(IllegalArgumentException.class); // 首字符必须字母数字

        // 合法面：单字符、连字符、32 位封顶
        assertThat(FileSandbox.forTenant(Path.of("."), "a").root().toString())
                .endsWith("tenants/a");
        assertThat(FileSandbox.forTenant(Path.of("."), "tenant-01").root().toString())
                .endsWith("tenants/tenant-01");
        assertThat(FileSandbox.forTenant(Path.of("."), "x".repeat(32)).root().toString())
                .endsWith("tenants/" + "x".repeat(32));
    }

    @Test
    void tenantSandboxHasNoAdditionalRoots(@TempDir Path root) {
        FileSandbox tenant = FileSandbox.forTenant(root, "gamma");
        // 无追加白名单：租户面严格窄于宿主面（宿主根下的直连路径也拒）
        assertThatThrownBy(() -> tenant.resolve(root.resolve("outside.txt").toString()))
                .isInstanceOf(SandboxViolationException.class);
        // 他租户已存在的实体路径（realpath 路径）同样拒
        Path betaHome = root.resolve("tenants").resolve("beta");
        assertThatThrownBy(() -> tenant.resolve(betaFile(root)))
                .isInstanceOf(SandboxViolationException.class);
    }

    private String betaFile(Path root) {
        return root.resolve("tenants").resolve("beta").resolve("secret").toString();
    }
}
