package com.crmbank.erp.hsip.service;

import com.crmbank.erp.hasl.mapper.HaslMapper;
import com.crmbank.erp.hsio.dto.Hsio100u;
import com.crmbank.erp.hsio.dto.Hsio101u;
import com.crmbank.erp.hsio.mapper.HsioMapper;
import com.crmbank.erp.hsip.dto.*;
import com.crmbank.erp.hsip.mapper.HsipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HsipService {

    private final HsipMapper hsipMapper;
    private final HsioMapper hsioMapper;
    private final HaslMapper haslMapper;

    /**
     * Map의 모든 Key를 소문자로 변환 (시스템 표준화)
     */
    private Map<String, Object> lower(Map<String, Object> map) {
        if (map == null) return new HashMap<>();
        Map<String, Object> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            newMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return newMap;
    }

    /**
     * 🚀 수입비용 전표대체 통합 저장 (트랜잭션 보장)
     */
    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip140(Hsip140uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        Map<String, Object> vat = request.getVat();
        List<Map<String, Object>> costs = request.getCosts();
        List<Map<String, Object>> payments = request.getPayments();

        String slipymd = nvl(mst.get("payymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String usernm = nvl(mst.get("usernm"));

        String autoSlip = "N";
        List<Map<String, Object>> resSet = hsipMapper.HSIP_112U_STR(Map.of("cmpycd", cmpycd, "gbn", "P1"));
        if (resSet != null && !resSet.isEmpty()) {
            autoSlip = nvl(lower(resSet.getFirst()).get("slipyn"), "N");
        }
        String acctymd = "Y".equals(autoSlip) ? slipymd : "";

        Map<String, Object> mParams = createParamMap();
        safePut(mParams, "actkind", "A"); safePut(mParams, "cmpycd", cmpycd); safePut(mParams, "slipymd", slipymd);
        safePut(mParams, "acctymd", acctymd); safePut(mParams, "deptcd", deptcd); safePut(mParams, "empnm", usernm);
        safePut(mParams, "slipgu", "031"); safePut(mParams, "slipkind", "031");
        safePut(mParams, "business", slipymd.substring(0, 4) + "년 " + slipymd.substring(4, 6) + "월 수입비용 대체");
        safePut(mParams, "updemp", userId);

        List<Map<String, Object>> resMst = haslMapper.HASL_010U_STR(mParams);
        if (resMst == null || resMst.isEmpty()) throw new Exception("전표 마스터 생성 실패");
        Map<String, Object> rowM = lower(resMst.getFirst());
        String slipno = nvl(rowM.get("slipno"));
        if (slipno.isEmpty() || "000000".equals(slipno)) throw new Exception("전표 번호 생성 실패");

        String firstCostNm = "";
        for (int i = 0; i < costs.size(); i++) {
            Map<String, Object> item = costs.get(i);
            if (i == 0) firstCostNm = nvl(item.get("costnm"));
            Map<String, Object> dParams = createParamMap();
            safePut(dParams, "actkind", "A"); safePut(dParams, "cmpycd", cmpycd); safePut(dParams, "slipymd", slipymd);
            safePut(dParams, "slipno", slipno); safePut(dParams, "acctymd", acctymd); safePut(dParams, "acctcd", "1365");
            safePut(dParams, "deptcd", nvl(item.get("deptcd")).isEmpty() ? deptcd : nvl(item.get("deptcd")));
            safePut(dParams, "mgtno", nvl(item.get("fileno"))); safePut(dParams, "dbamt", nvl(item.get("costamt"), "0"));
            safePut(dParams, "cramt", "0"); safePut(dParams, "remark", nvl(item.get("bigo"), nvl(item.get("costnm"))) + "(" + nvl(item.get("fileno")) + ")");
            safePut(dParams, "updemp", userId);
            checkResult(haslMapper.HASL_011U_STR(dParams), "비용 상세 저장");

            Map<String, Object> uParams = new HashMap<>();
            uParams.put("actkind", "U0"); uParams.put("cmpycd", cmpycd); uParams.put("deptcd", nvl(mst.get("search_deptcd")));
            uParams.put("fromdt", nvl(mst.get("fromdt")).replace("-", "")); uParams.put("todt", nvl(mst.get("todt")).replace("-", ""));
            uParams.put("fileno", nvl(item.get("fileno"))); uParams.put("docno", nvl(item.get("docno")));
            uParams.put("crowno", nvl(item.get("crowno"))); uParams.put("slipymd", slipymd); uParams.put("slipno", slipno);
            uParams.put("updemp", userId);
            hsipMapper.HSIP_140U_STR(uParams);
        }

        if (vat != null && !"000".equals(nvl(vat.get("vattype")))) {
            String vt = nvl(vat.get("vattype")).split("\\|")[0];
            Map<String, Object> vParams = createParamMap();
            safePut(vParams, "actkind", "A"); safePut(vParams, "cmpycd", cmpycd); safePut(vParams, "slipymd", slipymd);
            safePut(vParams, "slipno", slipno); safePut(vParams, "acctcd", "1275"); safePut(vParams, "deptcd", nvl(vat.get("taxunit"))); 
            safePut(vParams, "custcd", nvl(vat.get("custcd"))); safePut(vParams, "mgtno", costs.get(0).get("fileno"));
            safePut(vParams, "dbamt", nvl(vat.get("vatamt"), "0")); safePut(vParams, "cramt", "0");
            safePut(vParams, "remark", (costs.size() > 1 ? firstCostNm + " 외 부가세" : firstCostNm + " 부가세"));
            safePut(vParams, "docno1", nvl(vat.get("taxunit"))); safePut(vParams, "docno2", nvl(vat.get("custcd")));
            safePut(vParams, "docno3", vt); safePut(vParams, "docno6", slipymd);
            safePut(vParams, "docno8", nvl(mst.get("targetTotalAmt"), "0")); safePut(vParams, "docno9", nvl(vat.get("vatamt"), "0"));
            safePut(vParams, "updemp", userId);
            checkResult(haslMapper.HASL_011U_STR(vParams), "부가세 저장");
        }

        for (Map<String, Object> p : payments) {
            String acct = nvl(p.get("acctcd")); String cred = nvl(p.get("bankcd"), nvl(p.get("custcd")));
            Map<String, Object> pParams = createParamMap();
            safePut(pParams, "actkind", "A"); safePut(pParams, "cmpycd", cmpycd); safePut(pParams, "slipymd", slipymd);
            safePut(pParams, "slipno", slipno); safePut(pParams, "acctymd", acctymd); safePut(pParams, "acctcd", acct);
            safePut(pParams, "deptcd", deptcd); safePut(pParams, "custcd", cred); safePut(pParams, "mgtno", nvl(p.get("mgtno")));
            safePut(pParams, "dbamt", "0"); safePut(pParams, "cramt", nvl(p.get("payamt"), "0")); safePut(pParams, "remark", nvl(p.get("remark")));
            if ("1195".equals(acct) || "2115".equals(acct) || "2150".equals(acct)) {
                safePut(pParams, "docno1", nvl(p.get("mgtno"))); safePut(pParams, "docno3", "020");
                if (!"1195".equals(acct)) safePut(pParams, "docno8", nvl(p.get("payamt"), "0"));
                safePut(pParams, "docno9", cred);
            }
            safePut(pParams, "updemp", userId);
            checkResult(haslMapper.HASL_011U_STR(pParams), "대변 저장");
        }

        if ("Y".equals(autoSlip)) {
            Map<String, Object> cParams = createParamMap();
            safePut(cParams, "actkind", "A0"); safePut(cParams, "cmpycd", cmpycd); safePut(cParams, "slipymd", slipymd);
            safePut(cParams, "acctymd", acctymd); safePut(cParams, "slipno", slipno); safePut(cParams, "deptcd", deptcd);
            safePut(cParams, "slipkind", "031"); safePut(cParams, "cofmyn", "Y"); safePut(cParams, "cofmemp", usernm); safePut(cParams, "updemp", userId);
            haslMapper.HASL_020U_STR(cParams);
        }
        return Map.of("slipno", slipno, "res", "OK");
    }

    /**
     * 🚀 수입전표 통합 취소 처리 (ASP 로직 완벽 이식)
     */
    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> cancelSlips150(Hsip150uCancelRequest request, String userId) throws Exception {
        String cmpycd = request.getCmpycd();
        String autoSlip = nvl(request.getAutoSlip(), "N");
        String fromdt = nvl(request.getFromdt()).replace("-", "");
        String todt = nvl(request.getTodt()).replace("-", "");

        for (Map<String, Object> item : request.getItems()) {
            String slipymd = nvl(item.get("slipymd"));
            String slipno = nvl(item.get("slipno"));
            String udeptcd = nvl(item.get("deptcd"), nvl(item.get("udeptcd")));

            if ("Y".equals(autoSlip)) {
                Map<String, Object> haslParams = createParamMap();
                safePut(haslParams, "actkind", "A0"); safePut(haslParams, "cmpycd", cmpycd); safePut(haslParams, "slipymd", slipymd);
                safePut(haslParams, "acctymd", slipymd); safePut(haslParams, "slipno", slipno); safePut(haslParams, "deptcd", udeptcd);
                safePut(haslParams, "slipkind", "031"); safePut(haslParams, "slipyn", "Y"); safePut(haslParams, "cofmyn", "N");
                safePut(haslParams, "updemp", userId);
                haslMapper.HASL_020U_STR(haslParams);
            }

            Map<String, Object> hsipParams = new HashMap<>();
            hsipParams.put("actkind", "D0"); hsipParams.put("cmpycd", cmpycd); hsipParams.put("fromdt", fromdt);
            hsipParams.put("todt", todt); hsipParams.put("deptcd", udeptcd); hsipParams.put("slipymd", slipymd);
            hsipParams.put("slipno", slipno); hsipParams.put("updemp", userId);

            List<Map<String, Object>> res = hsipMapper.HSIP_150U_STR(hsipParams);
            if (res != null && !res.isEmpty()) {
                Map<String, Object> row = lower(res.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    String msg = nvl(row.get("msg"), "취소 업무 오류");
                    throw new Exception("전표[" + slipno + "] 취소 실패: " + msg);
                }
            }
        }
        return Map.of("res", "OK");
    }

    /**
     * 🚀 외부수입전표 통합 저장 처리 (HSIP145U)
     */
    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveExternalSlip145(Hsip145uSaveRequest request, String userId, String cmpycd, String deptcd) throws Exception {
        String pubymd = nvl(request.getPubymd()).replace("-", "");
        String ioymdfr = nvl(request.getIoymdfr()).replace("-", "");
        String ioymdto = nvl(request.getIoymdto()).replace("-", "");

        for (Map<String, Object> item : request.getItems()) {
            String costcd = nvl(item.get("costcd"));
            String itemFileno = nvl(item.get("fileno"), request.getFileno());
            String docno = nvl(item.get("docno"));
            String crowno = nvl(item.get("crowno"));
            String slipKind = "200".equals(costcd) ? "031" : "030";
            String itemDeptcd = nvl(item.get("deptcd"), deptcd);

            Map<String, Object> baseParams = new HashMap<>();
            baseParams.put("actkind", "A0");
            baseParams.put("cmpycd", cmpycd);
            baseParams.put("costcd", costcd);
            baseParams.put("fromdt", ioymdfr);
            baseParams.put("todt", ioymdto);
            baseParams.put("deptcd", itemDeptcd);
            baseParams.put("fileno", itemFileno);
            baseParams.put("docno", docno);
            baseParams.put("crowno", crowno);
            baseParams.put("jsanymd", pubymd);
            baseParams.put("spyamt", nvl(item.get("costamt"), "0"));
            baseParams.put("vatamt", nvl(item.get("vatamt"), "0"));
            baseParams.put("custcd", nvl(item.get("custcd")));
            baseParams.put("taxunit", nvl(item.get("taxunit"), "100"));
            baseParams.put("vattype", nvl(item.get("vattype"), "010"));
            baseParams.put("slipymd", pubymd);
            baseParams.put("slipno", "");
            baseParams.put("slipkind", slipKind);
            baseParams.put("hdeptcd", itemDeptcd);
            baseParams.put("business", itemFileno + "-" + nvl(item.get("bigo")));
            baseParams.put("updemp", userId);

            // Step 1: 전표번호 채번 (A0)
            List<Map<String, Object>> resA = hsipMapper.HSIP_145U_STR(baseParams);
            if (resA == null || resA.isEmpty()) throw new Exception("전표번호 채번 실패");
            Map<String, Object> rowA = lower(resA.getFirst());
            String slipno = nvl(rowA.get("slipno"));
            if (slipno.isEmpty() || "000000".equals(slipno)) throw new Exception("유효한 전표번호를 받지 못했습니다.");

            // Step 2: 정산 저장 (U0)
            baseParams.put("actkind", "U0");
            baseParams.put("slipno", slipno);
            List<Map<String, Object>> resU = hsipMapper.HSIP_145U_STR(baseParams);
            if (resU != null && !resU.isEmpty()) {
                Map<String, Object> rowU = lower(resU.getFirst());
                if ("00000000".equals(nvl(rowU.get("status")))) {
                    throw new Exception("정산 저장 실패: " + nvl(rowU.get("msg"), "업무 오류"));
                }
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveImportOrder(Hsip100uRequest request, String userId) throws Exception {
        Hsip100u mst = request.getMst();
        List<Map<String, Object>> res = hsipMapper.HSIP_100U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("Master Save Failed");
        Map<String, Object> mstRow = lower(res.getFirst());
        String fileno = nvl(mstRow.get("fileno"));
        if (request.getDtl() != null) {
            for (Hsip101u dtl : request.getDtl()) {
                dtl.setFileno(fileno); dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId); hsipMapper.HSIP_101U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveCustomsStock(Hsip120uSaveRequest request, String userId) throws Exception {
        Hsip120u mst = request.getMst(); List<Hsip121u> dtlList = request.getDtl();
        if (mst == null) throw new Exception("마스터 누락");
        String mstIono = nvl(mst.getIono()); String mstActKind = mstIono.isEmpty() ? "A" : "U";
        Hsio100u hsioMst = new Hsio100u();
        hsioMst.setActkind(mstActKind); hsioMst.setCmpycd(mst.getCmpycd()); hsioMst.setIogbn("100");
        hsioMst.setIoym(mst.getIoym()); hsioMst.setIono(mstIono); hsioMst.setIoymd(mst.getPassymd());
        hsioMst.setIotype("100"); hsioMst.setCustcd(mst.getCustcd()); hsioMst.setDeptcd(mst.getDeptcd());
        hsioMst.setWhcd(mst.getWhcd()); hsioMst.setCfmyn("Y"); hsioMst.setUpdemp(userId);
        hsioMst.setRemark(mst.getCustnm() + " 수입 통관 입고"); hsioMst.setTotsum("0");
        List<Map<String, Object>> resM = hsioMapper.HSIO_100U_STR(hsioMst);
        if (resM == null || resM.isEmpty()) throw new Exception("입고 마스터 저장 실패");
        Map<String, Object> rowM = lower(resM.getFirst());
        String keyIoym = nvl(rowM.get("ioym"));
        String keyIono = nvl(rowM.get("iono"));
        mst.setActkind(mstActKind + "0"); mst.setIono(keyIono); mst.setIoym(keyIoym); mst.setUpdemp(userId);
        hsipMapper.HSIP_120U_STR(mst);
        if (dtlList != null) {
            for (Hsip121u item : dtlList) {
                String iorowno = nvl(item.getIorowno());
                BigDecimal iqty = item.getIqty() != null ? item.getIqty() : BigDecimal.ZERO;
                BigDecimal totalQty = iqty.add(item.getGqty() != null ? item.getGqty() : BigDecimal.ZERO);
                Hsio101u hsioDtl = new Hsio101u();
                hsioDtl.setActkind(iorowno.isEmpty() ? "A" : (totalQty.compareTo(BigDecimal.ZERO) == 0 ? "D" : "U"));
                hsioDtl.setCmpycd(mst.getCmpycd()); hsioDtl.setIogbn("100"); hsioDtl.setIoym(keyIoym);
                hsioDtl.setIono(keyIono); hsioDtl.setIorowno(iorowno); hsioDtl.setIoymd(mst.getPassymd());
                hsioDtl.setCustcd(mst.getCustcd()); hsioDtl.setIotype("100"); hsioDtl.setDeptcd(mst.getDeptcd());
                hsioDtl.setWhcd(mst.getWhcd()); hsioDtl.setItemcd(nvl(item.getItemcd()));
                hsioDtl.setItsize(nvl(item.getItsize())); hsioDtl.setUnit(nvl(item.getUnit()));
                hsioDtl.setPkunit(nvl(item.getUnit())); hsioDtl.setIoqty(iqty);
                hsioDtl.setIoamt(item.getAmt() != null ? item.getAmt() : BigDecimal.ZERO);
                hsioDtl.setIovat(BigDecimal.ZERO); hsioDtl.setCfmyn("Y"); hsioDtl.setUpdemp(userId);
                List<Map<String, Object>> resD = hsioMapper.HSIO_101U_STR(hsioDtl);
                if (resD != null && !resD.isEmpty()) {
                    Map<String, Object> rowD = lower(resD.getFirst());
                    String savedIorowno = nvl(rowD.get("iorowno"));
                    item.setActkind(iorowno.isEmpty() ? "A0" : "U0"); item.setCmpycd(mst.getCmpycd());
                    item.setFileno(mst.getFileno()); item.setShipseq(mst.getShipseq());
                    item.setPassseq(mst.getPassseq()); item.setIoym(keyIoym); item.setIono(keyIono);
                    item.setIorowno(savedIorowno.isEmpty() ? iorowno : savedIorowno); item.setUpdemp(userId);
                    hsipMapper.HSIP_121U_STR(item);
                }
            }
        }
        return rowM;
    }

    private void checkResult(List<Map<String, Object>> res, String step) throws Exception {
        if (res != null && !res.isEmpty()) {
            Map<String, Object> row = lower(res.getFirst());
            String errYn = nvl(row.get("erryn"));
            if ("Y".equals(errYn)) throw new Exception(step + " 실패: " + nvl(row.get("msg")));
        }
    }

    private void safePut(Map<String, Object> map, String key, Object value) {
        map.put(key, value == null ? "" : String.valueOf(value).trim());
    }

    private String nvl(Object val) { return val == null ? "" : String.valueOf(val).trim(); }
    
    private String nvl(Object val, String def) {
        String s = nvl(val); return s.isEmpty() ? def : s;
    }

    private Map<String, Object> createParamMap() {
        Map<String, Object> map = new HashMap<>();
        String[] keys = { "actkind", "cmpycd", "slipymd", "slipno", "srowno", "acctymd", "acctcd", "deptcd", "custcd", "bugtcd", "prjcd", "mgtno", "sslipno", "dbamt", "cramt", "remark", "paycndt", "payymd", "docno1", "docno2", "docno3", "docno4", "docno5", "docno6", "docno7", "docno8", "docno9", "updemp", "frgnkind", "frgnrate", "frgnamt", "ret_yn", "taxunit", "custcd2", "taxtype", "slipymd2", "supyamt", "vatamt" };
        for (String k : keys) map.put(k, "");
        return map;
    }
}
