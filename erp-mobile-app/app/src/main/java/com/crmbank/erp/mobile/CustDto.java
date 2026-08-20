package com.crmbank.erp.mobile;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class CustDto {
    @SerializedName("cmpycd")
    public String cmpycd; // ?뚯궗肄붾뱶

    @SerializedName("nacd")
    public String nacd; // 援??肄붾뱶

    @SerializedName("schcustnm")
    public String schcustnm; // 寃??嫄곕옒泥섎챸

    @SerializedName("schcustgbn")
    public String schcustgbn; // 寃??嫄곕옒泥섍뎄遺?

    @SerializedName("schstatus")
    public String schstatus; // 寃???곹깭

    @SerializedName("custcd")
    public String custcd; // 嫄곕옒泥?

    @SerializedName("custno")
    public String custno; // ?ъ뾽?먮쾲??

    @SerializedName("jongcd")
    public String jongcd; // 醫낆궗?낆옣

    @SerializedName("custnm")
    public String custnm; // 嫄곕옒泥섎챸

    @SerializedName("custsnm")
    public String custsnm; // 嫄곕옒泥섎떒異뺣챸

    @SerializedName("custgbn")
    public String custgbn; // 嫄곕옒泥섍뎄遺?

    @SerializedName("bossnm")
    public String bossnm; // ??쒖옄

    @SerializedName("juminno")
    public String juminno; // 二쇰?踰덊샇

    @SerializedName("legalno")
    public String legalno; // 踰뺤씤踰덊샇

    @SerializedName("custkind")
    public String custkind; // ?낆쥌

    @SerializedName("custtype")
    public String custtype; // ?낇깭

    @SerializedName("telno")
    public String telno; // ?꾪솕

    @SerializedName("faxno")
    public String faxno; // ?⑹뒪

    @SerializedName("area")
    public String area; // 吏??

    @SerializedName("addrcd")
    public String addrcd; // 二쇱냼肄붾뱶

    @SerializedName("postno")
    public String postno; // ?고렪踰덊샇

    @SerializedName("address")
    public String address; // 二쇱냼

    @SerializedName("d_address")
    public String d_address; // ?곸꽭二쇱냼

    @SerializedName("stdymd")
    public String stdymd; // ?쒖옉?쇱옄

    @SerializedName("clsymd")
    public String clsymd; // 醫낅즺?쇱옄

    @SerializedName("status")
    public String status; // 嫄곕옒泥섏긽??

    @SerializedName("outcustcd")
    public String outcustcd; // ?붿〈嫄곕옒泥섏퐫??

    @SerializedName("int_agentyn")
    public String int_agentyn; // ?뚭컻?由ъ젏?щ?

    @SerializedName("sale_agentyn")
    public String sale_agentyn; // ?먮ℓ?由ъ젏?щ?

    @SerializedName("ma_agentyn")
    public String ma_agentyn; // ?좎?蹂댁닔?由ъ젏

    @SerializedName("adv_yn")
    public String adv_yn; // 愿묎퀬怨좉컼?щ?

    @SerializedName("repyn")
    public String repyn; // ?섎━??yn

    @SerializedName("useyn")
    public String useyn; // ?ъ슜?щ?

    @SerializedName("inprcgbn")
    public String inprcgbn; // 留ㅼ엯?④?

    @SerializedName("outprcgbn")
    public String outprcgbn; // 留ㅼ텧?④?

    @SerializedName("hdamt")
    public BigDecimal hdamt; // ?ъ떊?쒕룄

    @SerializedName("rcvdd")
    public BigDecimal rcvdd; // ?ъ떊湲고븳

    @SerializedName("agrpcd")
    public String agrpcd; // ?遺꾨쪟

    @SerializedName("bgrpcd")
    public String bgrpcd; // 以묐텇瑜?

    @SerializedName("cgrpcd")
    public String cgrpcd; // ?뚮텇瑜?

    @SerializedName("custuserno")
    public String custuserno; // ?대떦?먮쾲??

    @SerializedName("name")
    public String name; // ?대떦 ?깅챸

    @SerializedName("hpno")
    public String hpno; // ?대떦 ?곕씫泥?

    @SerializedName("useremail")
    public String useremail; // ?대떦??硫붿씪二쇱냼

    @SerializedName("email")
    public String email; // 硫붿씪二쇱냼

    @SerializedName("noscd")
    public String noscd; // 肄붾뱶

    @SerializedName("exists")
    public boolean exists;

    @SerializedName("addtime")
    public String addtime; // String?쇰줈 泥섎━ (LocalDateTime ???

    @SerializedName("updtime")
    public String updtime; // String?쇰줈 泥섎━

    @SerializedName("updemp")
    public String updemp;
}
