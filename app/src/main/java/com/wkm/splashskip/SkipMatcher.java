package com.wkm.splashskip;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure matching policy; deliberately never matches generic close/confirm buttons. */
public final class SkipMatcher {
    private static final Pattern LABEL = Pattern.compile(
        "^(?:[（(]?\\d{1,2}(?:[.,]\\d)?(?:s|秒)?[）)]?)?(?:跳过广告|跳過廣告|跳过|跳過|skipad(?:s)?|skip)(?:[（(]?\\d{1,2}(?:[.,]\\d)?(?:s|秒)?[）)]?)?$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTDOWN = Pattern.compile("\\d");
    private static final Pattern SPACE = Pattern.compile("[\\s\\u00a0\\u200b\\u3000|｜]");
    private static final Pattern ROLE = Pattern.compile("(?:按钮|button)$");
    private SkipMatcher() {}

    public static String normalize(String label) {
        return label == null ? "" : ROLE.matcher(SPACE.matcher(label).replaceAll("").toLowerCase(Locale.ROOT)).replaceAll("");
    }
    public static boolean possibleLabel(String label){
        if(label==null||label.isEmpty()||label.length()>48)return false;
        return label.contains("跳过")||label.contains("跳過")||label.toLowerCase(Locale.ROOT).contains("skip");
    }
    public static boolean sensitive(String text) {
        if (text == null) return false;
        String lower=text.toLowerCase(Locale.ROOT);
        return lower.contains("privacy policy") || lower.contains("verification code") || lower.contains("terms of use")
            || text.contains("隐私政策") || text.contains("用户协议") || text.contains("权限申请")
            || text.contains("实名认证") || text.contains("支付密码") || text.contains("验证码")
            || text.contains("安装未知应用") || text.contains("允许受限设置");
    }
    public static boolean adMarker(String text) {
        if (text == null || text.length() > 40) return false;
        String s = text.toLowerCase(Locale.ROOT);
        return s.contains("广告") || s.contains("廣告") || s.equals("ad") || s.equals("advertisement")
            || s.equals("sponsored") || s.contains("摇一摇");
    }
    public static int score(String label, String viewId, int left, int top, int width, int height,
                            int screenWidth, int screenHeight, float density,
                            boolean advertisementVisible, boolean sensitiveScreen, boolean enhanced) {
        if (sensitiveScreen || screenWidth <= 0 || screenHeight <= 0 || width <= 0 || height <= 0) return 0;
        if (!possibleLabel(label)) return 0;
        String text = normalize(label);
        if (text.length() > 22 || !LABEL.matcher(text).matches()) return 0;
        if (width > 190 * density || height > 80 * density) return 0;
        if (left < 0 || top < 0 || left + width > screenWidth + 2 || top + height > screenHeight + 2) return 0;
        boolean corner = left + width / 2 >= screenWidth * 0.56 && top + height / 2 <= screenHeight * 0.38;
        String id = viewId == null ? "" : viewId.toLowerCase(Locale.ROOT);
        boolean adId = id.contains("splash") || id.contains("skip_ad") || id.contains("ad_skip")
            || id.contains("ksad") || id.contains("tt_skip") || id.contains("gdt");
        boolean explicitAd = text.contains("广告") || text.contains("廣告") || text.contains("skipad");
        boolean countdown = COUNTDOWN.matcher(text).find();
        if (adId && (corner || explicitAd)) return 100;
        if (corner && (explicitAd || countdown || advertisementVisible)) return 90;
        if (corner && enhanced) return 60;
        return 0;
    }
}
