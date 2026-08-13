package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Firecracker 重载档（wayfinder2 impl-25 / T51：<b>接口预留</b>，实现按部署需求另接）：
 * microVM 硬件级隔离（36,040★）——jailer 全参（uid/gid/cgroup/chroot/netns/rlimit）、
 * 需 root + Linux + KVM（/dev/kvm）、musl 静态二进制、rootfs 准备为部署大头（7–10 天）。
 */
public final class FirecrackerSandbox implements CommandSandbox {

    @Override
    public String name() {
        return "firecracker";
    }

    @Override
    public boolean available() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux")
                && java.nio.file.Files.exists(java.nio.file.Path.of("/dev/kvm"));
    }

    @Override
    public String unavailableHint() {
        return "Firecracker 档需 Linux + KVM（/dev/kvm）+ jailer/rootfs 准备；"
                + "开发环境建议 DenoSandbox，托管场景建议 E2BSandbox。";
    }

    @Override
    public CommandResult run(List<String> command, Map<String, String> allowedEnv, Path workDir,
                             Duration timeout) {
        throw new UnsupportedOperationException(
                "FirecrackerSandbox 为接口预留档（实现按部署需求提供）；" + unavailableHint());
    }
}
