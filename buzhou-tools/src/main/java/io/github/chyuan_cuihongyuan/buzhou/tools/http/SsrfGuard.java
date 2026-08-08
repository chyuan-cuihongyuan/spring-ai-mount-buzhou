package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * SSRF 防护（spec 06 推演 #10）：默认拦内网段与云元数据端点，可配放行。
 *
 * <p>拦截清单：{@code 127.0.0.0/8}、{@code 10.0.0.0/8}、{@code 172.16.0.0/12}、
 * {@code 192.168.0.0/16}、{@code 169.254.0.0/16}（含云元数据 169.254.169.254）、
 * {@code 0.0.0.0/8}、{@code ::1} 与 {@code fc00::/7}；DNS 解析后对目标 IP 校验
 * （重定向逐跳校验与 DNS rebinding 防护属开放问题，当前承诺解析后校验一跳）。
 */
public class SsrfGuard {

    private record Cidr(int[] addressBytes, int prefixLength) {

        boolean matches(InetAddress address) {
            byte[] target = address.getAddress();
            if (target.length != addressBytes.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if ((target[i] & 0xFF) != (addressBytes[i] & 0xFF)) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits) & 0xFF;
            return (target[fullBytes] & mask) == (addressBytes[fullBytes] & mask);
        }

        static Cidr parse(String cidr) {
            int slash = cidr.indexOf('/');
            String host = slash < 0 ? cidr : cidr.substring(0, slash);
            int prefix = slash < 0 ? -1 : Integer.parseInt(cidr.substring(slash + 1));
            try {
                byte[] bytes = InetAddress.getByName(host).getAddress();
                int maxPrefix = bytes.length * 8;
                if (prefix > maxPrefix) {
                    throw new IllegalArgumentException(
                            "CIDR 前缀超地址位数（/" + prefix + " > /" + maxPrefix + "）：" + cidr);
                }
                if (prefix < 0 && slash >= 0) {
                    throw new IllegalArgumentException("CIDR 前缀非法：" + cidr);
                }
                int[] unsigned = new int[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    unsigned[i] = bytes[i] & 0xFF;
                }
                return new Cidr(unsigned, prefix < 0 ? maxPrefix : prefix);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("非法 CIDR/主机：" + cidr, e);
            }
        }
    }

    private static final List<String> DEFAULT_BLOCKED = List.of(
            "127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
            "169.254.0.0/16", "0.0.0.0/8", "::1/128", "fc00::/7");

    private final List<Cidr> blocked;
    private final List<Cidr> allowlist;
    private final List<String> allowedHosts;

    public SsrfGuard(boolean blockPrivateRanges, List<String> allowlistEntries) {
        this.blocked = blockPrivateRanges
                ? DEFAULT_BLOCKED.stream().map(Cidr::parse).toList() : List.of();
        List<Cidr> cidrs = new java.util.ArrayList<>();
        List<String> hosts = new java.util.ArrayList<>();
        for (String entry : allowlistEntries == null ? List.<String>of() : allowlistEntries) {
            if (entry.contains("/") || entry.contains(":")) {
                cidrs.add(Cidr.parse(entry));
            } else {
                hosts.add(entry.toLowerCase());
            }
        }
        this.allowlist = List.copyOf(cidrs);
        this.allowedHosts = List.copyOf(hosts);
    }

    public static SsrfGuard defaults() {
        return new SsrfGuard(true, List.of());
    }

    /**
     * 校验目标主机：主机名在放行清单直接通过；否则 DNS 解析后逐 IP 校验——
     * <b>每个</b>解析 IP 均须「命中放行网段或不命中拦截段」，任一 IP 被拦即整体拒绝
     * （防混合应答绕过：连接实际使用的 IP 由解析结果任选，不能只验其一）；
     * 解析失败也拒，fail-closed。
     *
     * @return null = 放行；非 null = 拒绝原因
     */
    public String check(String host) {
        if (host == null || host.isBlank()) {
            return "目标主机为空";
        }
        if (allowedHosts.contains(host.toLowerCase())) {
            return null;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return "DNS 解析失败（按拒绝处理）：" + host;
        }
        for (InetAddress address : addresses) {
            if (!(address instanceof Inet4Address) && !(address instanceof Inet6Address)) {
                continue;
            }
            if (allowlist.stream().anyMatch(c -> c.matches(address))) {
                continue;
            }
            for (Cidr cidr : blocked) {
                if (cidr.matches(address)) {
                    return "SSRF 拦截：目标地址命中内网/元数据拦截段 " + host + " -> "
                            + address.getHostAddress();
                }
            }
        }
        return null;
    }
}
