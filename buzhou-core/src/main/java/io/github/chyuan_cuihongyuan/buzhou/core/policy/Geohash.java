package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * Geohash 地理哈希（spec 6021 / T6235 / impl 2221）——
 * Redis GEO/位置服务 geohash 思想：**经纬度二分交织的
 * Base32 网格编码**——经度位（偶数位）与纬度位（奇数位）
 * 交织，每 5 位一字符（Base32），前缀共享即空间邻近——
 * 「两点距离阈值邻近判断」需逐对计算（无法用索引前缀剪枝）
 * 的病解。decode 还原包围盒（中心点+边界），encode/decode
 * 互逆定构（同点同精度同串）。
 *
 * <p>与 ZOrderCurve（recovery）同族不同面：经纬网格串编码
 * vs 平面坐标填充序。参数域 fail-fast（lat∈[-90,90]、
 * lon∈[-180,180]、精度 1–12、Base32 字符集）。
 */
public final class Geohash {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int MAX_PRECISION = 12;

    private Geohash() {
    }

    /** 编码（纬度/经度域外或精度越域 fail-fast）。 */
    public static String encode(double latitude, double longitude, int precision) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("纬度域 [-90,90]: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("经度域 [-180,180]: " + longitude);
        }
        if (precision < 1 || precision > MAX_PRECISION) {
            throw new IllegalArgumentException("精度须在 [1,12]: " + precision);
        }
        double latMin = -90.0;
        double latMax = 90.0;
        double lonMin = -180.0;
        double lonMax = 180.0;
        StringBuilder hash = new StringBuilder(precision);
        int bit = 0;
        int chunk = 0;
        boolean isLongitude = true;
        while (hash.length() < precision) {
            if (isLongitude) {
                double mid = (lonMin + lonMax) / 2;
                if (longitude >= mid) {
                    chunk = chunk << 1 | 1;
                    lonMin = mid;
                } else {
                    chunk = chunk << 1;
                    lonMax = mid;
                }
            } else {
                double mid = (latMin + latMax) / 2;
                if (latitude >= mid) {
                    chunk = chunk << 1 | 1;
                    latMin = mid;
                } else {
                    chunk = chunk << 1;
                    latMax = mid;
                }
            }
            isLongitude = !isLongitude;
            bit++;
            if (bit == 5) {
                hash.append(BASE32.charAt(chunk));
                bit = 0;
                chunk = 0;
            }
        }
        return hash.toString();
    }

    /** 包围盒（含 center——解码还原）。 */
    public record Box(double latMin, double latMax, double lonMin, double lonMax) {

        public double centerLatitude() {
            return (latMin + latMax) / 2;
        }

        public double centerLongitude() {
            return (lonMin + lonMax) / 2;
        }
    }

    /** 解码包围盒（null/非法字符 fail-fast）。 */
    public static Box decode(String hash) {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("hash 非空");
        }
        double latMin = -90.0;
        double latMax = 90.0;
        double lonMin = -180.0;
        double lonMax = 180.0;
        boolean isLongitude = true;
        for (int i = 0; i < hash.length(); i++) {
            int value = BASE32.indexOf(Character.toLowerCase(hash.charAt(i)));
            if (value < 0) {
                throw new IllegalArgumentException("非法 Base32 字符: " + hash.charAt(i));
            }
            for (int b = 4; b >= 0; b--) {
                if (isLongitude) {
                    double mid = (lonMin + lonMax) / 2;
                    if ((value >>> b & 1) == 1) {
                        lonMin = mid;
                    } else {
                        lonMax = mid;
                    }
                } else {
                    double mid = (latMin + latMax) / 2;
                    if ((value >>> b & 1) == 1) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                isLongitude = !isLongitude;
            }
        }
        return new Box(latMin, latMax, lonMin, lonMax);
    }
}
