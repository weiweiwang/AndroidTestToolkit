package android.weiweiwang.github.toolkit.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by weiwei on 15/3/14.
 */
public class ICCID {
    public final Set<String> OPERATOR_CHINA_MOBILE = new HashSet<String>() {{
        add("00");
        add("02");
        add("07");
    }};
    public final Set<String> OPERATOR_CHINA_UNICOM = new HashSet<String>() {{
        add("01");
        add("06");
    }};
    public final Set<String> OPERATOR_CHINA_TELECOM = new HashSet<String>() {{
        add("03");
        add("05");
    }};

    /**
     * 01 北京        02 天津        03 河北        04 山西        05 内蒙古        06 辽宁        07 吉林        08 黑龙江    09 上海         l0 江苏        11 浙江        12 安徽        13 福建        14 江西        15 山东        16 河南        17 湖北        18 湖南         l9 广东         20 广西        21 海南        22 四川        23 贵州        24 云南        25 西藏        26 陕西        27 甘肃        28 青海        29 宁夏       30 新疆        31 重庆
     */
    public final Map<String, String> CHINA_MOBILE_SS_MAP = new HashMap<String, String>() {{
        put("01", "北京");
        put("02", "天津");
        put("03", "河北");
        put("04", "山西");
        put("05", "内蒙古");
        put("06", "辽宁");
        put("07", "吉林");
        put("08", "黑龙江");
        put("09", "上海");
        put("10", "江苏");
        put("11", "浙江");
        put("12", "安徽");
        put("13", "福建");
        put("14", "江西");
        put("15", "山东");
        put("16", "河南");
        put("17", "湖北");
        put("18", "湖南");
        put("19", "广东");
        put("20", "广西");
        put("21", "海南");
        put("22", "四川");
        put("23", "贵州");
        put("24", "云南");
        put("25", "西藏");
        put("26", "陕西");
        put("27", "甘肃");
        put("28", "青海");
        put("29", "宁夏");
        put("30", "新疆");
        put("31", "重庆");
    }};

    public final Map<String, String> CHINA_UNICOM_SS_MAP = new HashMap<String, String>() {{
        put("10", "内蒙古");
        put("11", "北京");
        put("13", "天津");
        put("17", "山东");
        put("18", "河北");
        put("19", "山西");
        put("30", "安徽");
        put("31", "上海");
        put("34", "江苏");
        put("36", "浙江");
        put("38", "福建");
        put("50", "海南");
        put("51", "广东");
        put("59", "广西");
        put("70", "青海");
        put("71", "湖北");
        put("74", "湖南");
        put("75", "江西");
        put("76", "河南");
        put("79", "西藏");
        put("81", "四川");
        put("83", "重庆");
        put("84", "陕西");
        put("85", "贵州");
        put("86", "云南");
        put("87", "甘肃");
        put("88", "宁夏");
        put("89", "新疆");
        put("90", "吉林");
        put("91", "辽宁");
        put("97", "黑龙江");
    }};

    private String intl;
    private String country;
    private String operator;
    private String operatorName;
    private String province;
    private String provinceName = "北京";
    private Map<String, String> codeDistrictMap =null;

    /**
     * 移动：89 86 00 7 1 01 1000219706
     * <p/>
     * 电信：89  86  03  0   99 101 07788712
     * <p/>
     * 联通：89 86 01 11 8 110 14185042
     * <p/>
     * <p/>
     * 联通：89 86 01 13 0 188 00560671
     *
     * @param iccid
     */
    public ICCID(String iccid,Map<String, String> codeDistrictMap ) {
        this.codeDistrictMap = codeDistrictMap;
        intl = iccid.substring(0, 2);
        country = iccid.substring(2, 4);
        operator = iccid.substring(4, 6);
        if (OPERATOR_CHINA_MOBILE.contains(operator)) {
            province = iccid.substring(8, 10);
            if (CHINA_MOBILE_SS_MAP.containsKey(province)) {
                provinceName = CHINA_MOBILE_SS_MAP.get(province);
            }
            operatorName = "中国移动";
        } else if (OPERATOR_CHINA_UNICOM.contains(operator)) {
            province = iccid.substring(9, 11);
            if (CHINA_UNICOM_SS_MAP.containsKey(province)) {
                provinceName = CHINA_UNICOM_SS_MAP.get(province);
            }
            operatorName = "中国联通";
        } else if (OPERATOR_CHINA_TELECOM.contains(operator)) {
            province = iccid.substring(10, 13);
            provinceName = guessProvince(province);
            operatorName = "中国电信";
        }
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public String guessProvince(String provinceCode) {
        StringBuilder code = new StringBuilder();
        if (provinceCode.startsWith("0")) {
            code.append(provinceCode);
        } else {
            code.append("0").append(provinceCode);
        }
        if (codeDistrictMap.containsKey(code.toString())) {
            return codeDistrictMap.get(code.toString());
        }
        return provinceCode;
    }

    public String getIntl() {
        return intl;
    }

    public String getCountry() {
        return country;
    }

    public String getOperator() {
        return operator;
    }

    public String getProvince() {
        return province;
    }
}