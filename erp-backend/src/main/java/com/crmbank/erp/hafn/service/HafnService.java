package com.crmbank.erp.hafn.service;

import com.crmbank.erp.hasl.mapper.HaslMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HafnService {

    private final HaslMapper haslMapper;

    /**
     * HAFN_610U_SAVE: 자금수지결과 저장
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveHafn610(Map<String, Object> payload, String cmpycd, String userId) throws Exception {
        return processSaveSlip(payload, cmpycd, userId);
    }

    /**
     * HAFN_620U_SAVE: 자금수지결과 저장(월간)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveHafn620(Map<String, Object> payload, String cmpycd, String userId) throws Exception {
        return processSaveSlip(payload, cmpycd, userId);
    }

    /**
     * HAFN_630U_SAVE: 자금수지결과 저장(년간)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveHafn630(Map<String, Object> payload, String cmpycd, String userId) throws Exception {
        return processSaveSlip(payload, cmpycd, userId);
    }

    private Map<String, Object> processSaveSlip(Map<String, Object> payload, String cmpycd, String userId) throws Exception {
        Map<String, Object> master = (Map<String, Object>) payload.get("master");
        List<Map<String, Object>> details = (List<Map<String, Object>>) payload.get("details");
        String actkind = (String) payload.get("actkind");

        injectUserInfo(master, cmpycd, userId);
        master.put("actkind", actkind);

        // 1. 마스터 저장 (HASL_110U_STR 호출)
        List<Map<String, Object>> masterResult = haslMapper.HASL_110U_STR(master);
        if (masterResult == null || masterResult.isEmpty()) throw new Exception("마스터 저장 실패 (반환값 없음)");
        
        List<Object> mstValues = new ArrayList<>(masterResult.get(0).values());
        String slipno = String.valueOf(mstValues.get(0)).trim();
        
        if ("000000".equals(slipno)) {
            String errorMsg = mstValues.size() > 1 ? String.valueOf(mstValues.get(1)) : "마스터 저장 오류";
            throw new Exception(errorMsg);
        }

        String slipymd = String.valueOf(master.get("slipymd"));

        // 2. 상세 저장
        if (details != null) {
            for (Map<String, Object> detail : details) {
                injectUserInfo(detail, cmpycd, userId);
                detail.put("slipymd", slipymd);
                detail.put("slipno", slipno);
                detail.put("acctymd", master.get("acctymd"));
                detail.put("actkind", actkind);

                List<Map<String, Object>> detRes = haslMapper.HASL_111U_STR(detail);
                if (detRes != null && !detRes.isEmpty()) {
                    List<Object> dtlValues = new ArrayList<>(detRes.get(0).values());
                    if ("Y".equalsIgnoreCase(String.valueOf(dtlValues.get(0)))) {
                        throw new Exception(String.valueOf(dtlValues.get(1)));
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("slipno", slipno);
        return result;
    }

    private void injectUserInfo(Map<String, Object> params, String cmpycd, String userId) {
        if (params.get("cmpycd") == null || params.get("cmpycd").toString().trim().isEmpty()) {
            params.put("cmpycd", cmpycd);
        }
        if (params.get("userid") == null || params.get("userid").toString().trim().isEmpty()) {
            params.put("userid", userId);
        }
        params.put("updemp", userId);
    }
}
