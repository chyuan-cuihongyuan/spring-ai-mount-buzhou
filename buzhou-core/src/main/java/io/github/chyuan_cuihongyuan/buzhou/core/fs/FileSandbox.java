package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件根目录沙箱（spec 06 安全边界默认）：read_file / write_file / copy_file / str_replace /
 * run_command workdir 共用。
 *
 * <p>root 默认 = 应用工作目录；{@code allowed-paths} 白名单可追加。拒绝路径逃逸：
 * {@code ..} 解析后越界即拒；符号链接按 realpath 解析后再校验边界，防软链逃逸（spec 推演 #8）。
 *
 * <p>归属说明：原实现生于 buzhou-spill（ticket 10/24），ticket 16 原子工具包需要同一能力而
 * feature 模块间禁止直接依赖（09 模块工程档白名单），故上移至 core 公共包；
 * buzhou-spill 保留同名壳类委托本类以兼容既有引用。
 */
public class FileSandbox {

    private static final Logger LOG = LoggerFactory.getLogger(FileSandbox.class);

    private final Path root;
    private final List<Path> allowedRoots;

    public FileSandbox(Path root, List<Path> additionalAllowedRoots) {
        this.root = realpathOrAbsolute(root);
        this.allowedRoots = (additionalAllowedRoots == null ? List.<Path>of() : additionalAllowedRoots)
                .stream().map(FileSandbox::realpathOrAbsolute).toList();
    }

    /**
     * 租户隔离沙箱（spec 125 §A / T451，Milvus partition-key / OS chroot-per-tenant
     * 思想）：root 下 {@code tenants/<tenant>} 子根，<b>不带任何追加白名单</b>——
     * 租户面严格窄于宿主面，跨租户路径（{@code ../tenants/<他租户>}）被既有边界
     * 检查拒绝。
     *
     * <p>租户 id 白名单：{@code [a-z0-9][a-z0-9-]{0,31}}（DNS 标签风——无分隔符、
     * 无 {@code ..}、无大小写折叠歧义；越界即 fail-fast，租户维度天生进路径，不
     * 校验就是穿越漏洞）。
     */
    public static FileSandbox forTenant(Path root, String tenant) {
        if (tenant == null || !tenant.matches("[a-z0-9][a-z0-9-]{0,31}")) {
            throw new IllegalArgumentException(
                    "tenant id must match [a-z0-9][a-z0-9-]{0,31}: " + tenant);
        }
        Path base = root == null ? Path.of(".").toAbsolutePath().normalize()
                : root.toAbsolutePath().normalize();
        return new FileSandbox(base.resolve("tenants").resolve(tenant), List.of());
    }

    public Path root() {
        return root;
    }

    public Path resolve(String raw) {
        Path candidate = absolutize(raw);
        Path check = Files.exists(candidate) ? realpath(candidate) : candidate;
        if (!contains(check)) {
            throw violation("路径越出沙箱：" + raw);
        }
        return check;
    }

    public Path resolveForWrite(String raw) {
        Path candidate = absolutize(raw);
        Path parent = candidate.getParent();
        if (parent == null) {
            throw violation("路径越出沙箱：" + raw);
        }
        Path realParent = Files.exists(parent) ? realpath(parent) : parent;
        Path resolved = realParent.resolve(candidate.getFileName().toString());
        if (!contains(resolved)) {
            throw violation("路径越出沙箱：" + raw);
        }
        return resolved;
    }

    public boolean contains(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return true;
        }
        return allowedRoots.stream().anyMatch(normalized::startsWith);
    }

    private Path absolutize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw violation("路径为空");
        }
        Path path = Path.of(raw);
        if (!path.isAbsolute()) {
            path = root.resolve(path);
        }
        return path.normalize();
    }

    /** ticket 29 日志基线：沙箱拒绝统一经此告警（spec 13 §cross-11）后抛出，拒绝可观测。 */
    private static SandboxViolationException violation(String message) {
        LOG.warn("沙箱拒绝：{}", message);
        return new SandboxViolationException(message);
    }

    private static Path realpath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw violation("路径解析失败：" + path);
        }
    }

    private static Path realpathOrAbsolute(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }
}
