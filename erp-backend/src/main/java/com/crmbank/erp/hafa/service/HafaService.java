package com.crmbank.erp.hafa.service;

import com.crmbank.erp.hasl.mapper.HaslMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HafaService {

    private final HaslMapper haslMapper;

    /**
     * 감가상각 전표 발행 처리 (HAFA_150U_SAVE)
     * ASP 전표 발행 로직을 Spring Boot 서비스로 이관
     */
    @Transactional
    public Map<String, Object> saveDepreciationSlip(Map<String, Object> params) {
        String cmpycd = (String) params.get("cmpycd");
        String mm = String.valueOf(params.get("mm"));
        String deptcd = (String) params.get("deptcd"); // 발행부서
        String acctymd = String.valueOf(params.get("acctymd")).replace("-", ""); // 발행일자
        String userid = (String) params.get("userid");
        String usernm = (String) params.get("usernm");
        List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");

        String sysDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1. 전표 마스터 생성 (HASL_110U_STR)
        Map<String, Object> masterParams = new HashMap<>();
        masterParams.put("actkind", "A0");
        masterParams.put("cmpycd", cmpycd);
        masterParams.put("fromdt", sysDate);
        masterParams.put("todt", ""); // PSLIPNO (초기값 빈값)
        masterParams.put("keyword", acctymd);
        masterParams.put("slipymd", deptcd);
        masterParams.put("slipno", usernm);
        masterParams.put("acctymd", "020");
        masterParams.put("deptcd", mm + "월분 감가상각비 계상");
        masterParams.put("empnm", userid);
        masterParams.put("slipgu", null);
        masterParams.put("updemp", null);

        List<Map<String, Object>> masterResult = haslMapper.HASL_110U_STR(masterParams);
        if (masterResult == null || masterResult.isEmpty()) {
            throw new RuntimeException("전표 마스터 생성에 실패하였습니다.");
        }

        // 프로시저에서 생성된 전표번호(PSLIPNO) 가져오기
        Map<String, Object> firstRow = masterResult.get(0);
        String pslipno = firstRow.values().iterator().next().toString();

        // 2. 항목별 차변/대변 생성 (HASL_111U_STR)
        for (Map<String, Object> item : items) {
            String costtype = String.valueOf(item.get("costtype"));
            String udeptcd = String.valueOf(item.get("deptcd")); // 자산소속부서
            String acctnm = String.valueOf(item.get("acctnm"));
            String dprsamt = String.valueOf(item.get("dprsamt")).replace(",", "");
            
            String sacctcd = String.valueOf(item.get("sacctcd"));
            String macctcd = String.valueOf(item.get("macctcd"));
            String cacctcd = String.valueOf(item.get("cacctcd"));

            // ASP 로직: COSTTYPE이 '010'이면 SACCTCD, 아니면 MACCTCD
            String acctcd = "010".equals(costtype) ? sacctcd : macctcd;
            String remark = mm + "월분 " + acctnm + " 감가상각비";

            // [차변] 생성
            Map<String, Object> debitParams = createDetailParams("A0", cmpycd, sysDate, pslipno, acctymd, acctcd, udeptcd, dprsamt, "0", remark, userid);
            checkAndThrow(haslMapper.HASL_111U_STR(debitParams));

            // [대변] 생성
            Map<String, Object> creditParams = createDetailParams("A0", cmpycd, sysDate, pslipno, acctymd, cacctcd, udeptcd, "0", dprsamt, remark, userid);
            checkAndThrow(haslMapper.HASL_111U_STR(creditParams));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("pslipno", pslipno);
        return result;
    }

    private Map<String, Object> createDetailParams(String actkind, String cmpycd, String sysDate, String slipno, String acctymd, String acctcd, String deptcd, String dbamt, String cramt, String remark, String userid) {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", actkind);
        p.put("cmpycd", cmpycd);
        p.put("slipymd", sysDate);
        p.put("slipno", slipno);
        p.put("srowno", null);
        p.put("acctymd", acctymd);
        p.put("acctcd", acctcd);
        p.put("deptcd", deptcd);
        p.put("custcd", "");
        p.put("bugtcd", "");
        p.put("prjcd", "");
        p.put("mgtno", "");
        p.put("sslipno", "");
        p.put("dbamt", dbamt);
        p.put("cramt", cramt);
        p.put("remark", remark);
        p.put("paycndt", "");
        p.put("payymd", "");
        p.put("docno1", "");
        p.put("docno2", "");
        p.put("docno3", "");
        p.put("docno4", "");
        p.put("docno5", "");
        p.put("docno6", "");
        p.put("docno7", "");
        p.put("docno8", "");
        p.put("docno9", "");
        p.put("updemp", userid);
        p.put("frgnkind", "");
        p.put("frgnrate", "0");
        p.put("frgnamt", "0");
        p.put("ret_yn", null);
        return p;
    }

    private void checkAndThrow(List<Map<String, Object>> result) {
        if (result != null && !result.isEmpty()) {
            Map<String, Object> row = result.get(0);
            List<Object> values = new ArrayList<>(row.values());
            String status = String.valueOf(values.get(0)).trim();
            
            if ("000000".equals(status)) {
                String errMsg = "작업 처리 중 오류가 발생했습니다.";
                if (values.size() > 1) {
                    errMsg = String.valueOf(values.get(1));
                }
                throw new RuntimeException(errMsg);
            }
        }
    }
}
