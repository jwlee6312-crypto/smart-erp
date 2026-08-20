package com.crmbank.erp.hppl.service;

import com.crmbank.erp.hppl.dto.PdPlanDto;
import com.crmbank.erp.hppl.mapper.PdPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdPlanService {

    private final PdPlanMapper pdPlanMapper;
    private final com.crmbank.erp.hppl.mapper.HpplMapper hpplMapper;

    public List<PdPlanDto> getPdPlanTargetList(Map<String, Object> params) {
        return pdPlanMapper.getPdPlanTargetList(params);
    }

    public List<PdPlanDto> getPdPlanList(Map<String, Object> params) {
        return pdPlanMapper.getPdPlanList(params);
    }

    public List<PdPlanDto> getPdPlanMonthlyList(Map<String, Object> params) {
        return pdPlanMapper.getPdPlanMonthlyList(params);
    }

    public List<Map<String, Object>> getPdPlanMonthlySummary(Map<String, Object> params) {
        return pdPlanMapper.getPdPlanMonthlySummary(params);
    }

    /**
     * 양산/외주 요청 자료 조회 (전체 내역)
     */
    public List<Map<String, Object>> getPdRequestList(Map<String, Object> params) {
        params.put("actkind", "S0");
        params.put("fromdt", params.get("frymd"));
        params.put("todt", params.get("toymd"));
        params.put("custnm", params.get("custnm") != null ? params.get("custnm") : "");
        params.put("reqymd", "");
        params.put("reqym", "");
        params.put("reqno", "");
        params.put("custcd", "");
        params.put("napgiymd", "");
        params.put("itemcd", "");
        params.put("planqty", 0);
        params.put("bigo", "");
        params.put("pumymd", "");
        params.put("pumno", "");
        params.put("updemp", "");

        return hpplMapper.HPPL_150U_STR(params);
    }

    /**
     * 양산/외주 요청 자료 일괄 저장
     */
    @Transactional
    public void savePdRequestList(List<PdPlanDto> list, String cmpycd, String userId) {
        for (PdPlanDto dto : list) {
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            params.put("gubun", dto.getGubun());
            params.put("fromdt", "");
            params.put("todt", "");
            params.put("custnm", "");
            params.put("updemp", userId);

            String status = dto.get_status();
            if ("입력".equals(status)) params.put("actkind", "A0");
            else if ("수정".equals(status)) params.put("actkind", "U0");
            else if ("삭제".equals(status)) params.put("actkind", "D0");
            else continue;

            params.put("reqymd", dto.getReqymd() != null ? dto.getReqymd().replace("-", "") : "");
            params.put("reqym", dto.getReqym() != null ? dto.getReqym().replace("-", "") : "");
            params.put("reqno", dto.getReqno() != null ? dto.getReqno() : "");
            params.put("custcd", dto.getCustcd() != null ? dto.getCustcd() : "");
            params.put("napgiymd", dto.getNapgiymd() != null ? dto.getNapgiymd().replace("-", "") : "");
            params.put("itemcd", dto.getItemcd() != null ? dto.getItemcd() : "");
            params.put("planqty", dto.getPlanqty() != null ? dto.getPlanqty() : 0);
            params.put("bigo", dto.getBigo() != null ? dto.getBigo() : "");
            params.put("pumymd", dto.getPumymd() != null ? dto.getPumymd().replace("-", "") : "");
            params.put("pumno", dto.getPumno() != null ? dto.getPumno() : "");

            hpplMapper.HPPL_150U_STR(params);
        }
    }

    /**
     * 주간생산계획 일괄 저장 (일반 자바 프로그램 방식)
     * _status 값에 따라 Insert, Update, Delete를 분기 처리
     */
    @Transactional
    public void savePdPlanList(List<PdPlanDto> list, String cmpycd, String userId) {
        String safeUserId = (userId != null && userId.length() > 20) ? userId.substring(0, 20) : userId;

        for (PdPlanDto dto : list) {
            String status = dto.get_status();
            if (status == null || status.isEmpty()) {
                String state = dto.getState();
                if ("C".equals(state)) status = "입력";
                else if ("U".equals(state)) status = "수정";
                else if ("D".equals(state)) status = "삭제";
            }
            if (status == null || status.isEmpty()) continue;

            dto.setCmpycd(cmpycd);
            dto.setUpdemp(safeUserId);

            // 날짜 및 년월 가공 (하이픈 제거)
            if (dto.getYymmdd() != null) dto.setYymmdd(dto.getYymmdd().replace("-", ""));
            if (dto.getNapgiymd() != null) dto.setNapgiymd(dto.getNapgiymd().replace("-", ""));
            if (dto.getOrdymd() != null) dto.setOrdymd(dto.getOrdymd().replace("-", ""));
            if (dto.getOrdym() != null) dto.setOrdym(dto.getOrdym().replace("-", ""));

            // 주문번호(ORDNO) 자릿수 체크 및 기본값 설정
            if (dto.getOrdno() == null || dto.getOrdno().trim().isEmpty()) {
                dto.setOrdno("0000");
            } else if (dto.getOrdno().length() > 4) {
                throw new RuntimeException("주문번호(ORDNO)는 4자리를 초과할 수 없습니다: " + dto.getOrdno());
            }

            // 지시년월(ORDYM) 기본값 설정
            if (dto.getOrdym() == null || dto.getOrdym().trim().isEmpty()) {
                dto.setOrdym(dto.getOrdymd() != null && dto.getOrdymd().length() >= 6 ? dto.getOrdymd().substring(0, 6) : "000000");
            }

            if (dto.getOrowno() == null || dto.getOrowno().trim().isEmpty()) dto.setOrowno("000");
            if (dto.getCustcd() == null) dto.setCustcd("");

            // 숫자형 필드 기본값 (NULL 방지)
            if (dto.getOrdqty() == null) dto.setOrdqty(java.math.BigDecimal.ZERO);
            if (dto.getPlanqty() == null) dto.setPlanqty(java.math.BigDecimal.ZERO);
            if (dto.getSuppamt() == null) dto.setSuppamt(java.math.BigDecimal.ZERO);

            try {
                if ("입력".equals(status)) {
                    // 중복 생성 방지 체크
                    Map<String, Object> chkParams = new HashMap<>();
                    chkParams.put("cmpycd", dto.getCmpycd());
                    chkParams.put("ordym", dto.getOrdym());
                    chkParams.put("ordno", dto.getOrdno());
                    chkParams.put("orowno", dto.getOrowno());
                    
                    if (pdPlanMapper.selectPdPlanCount(chkParams) > 0) {
                        log.warn("⚠️ 이미 계획이 수립된 항목입니다: {} - {}", dto.getOrdno(), dto.getOrowno());
                        continue;
                    }

                    pdPlanMapper.insertPdPlan(dto);
                }
                else if ("수정".equals(status)) {
                    pdPlanMapper.updatePdPlan(dto);
                }
                else if ("삭제".equals(status)) {
                    Map<String, Object> delParams = new HashMap<>();
                    delParams.put("cmpycd", cmpycd);
                    delParams.put("yymmdd", dto.getYymmdd());
                    delParams.put("ser", dto.getSer());
                    pdPlanMapper.deletePdPlan(delParams);
                }
            } catch (Exception e) {
                log.error("❌ 생산계획 처리 오류: {}", e.getMessage());
                throw new RuntimeException("생산계획 저장 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void deletePdPlan(Map<String, Object> params) {
        pdPlanMapper.deletePdPlan(params);
    }

    /**
     * 월간 생산계획 일자 이동 (자바 방식: 삭제 후 재등록)
     */
    @Transactional
    public void movePdPlanDate(Map<String, Object> params) {
        // 1. 기존 계획 조회 (oldyymmdd, oldser 사용)
        PdPlanDto oldPlan = pdPlanMapper.selectPdPlan(params);
        if (oldPlan == null) throw new RuntimeException("이동할 계획 데이터를 찾을 수 없습니다.");
        
        // 🚀 [보안] 서버측에서도 작업지시 여부 재검증
        if (oldPlan.getOrdqty() != null && oldPlan.getOrdqty().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new RuntimeException("이미 작업지시가 진행된 계획은 일자를 이동할 수 없습니다.");
        }

        // 2. 기존 계획 삭제 (cmpycd, yymmdd, ser 명시적 매핑)
        Map<String, Object> delParams = new HashMap<>();
        delParams.put("cmpycd", params.get("cmpycd"));
        delParams.put("yymmdd", params.get("oldyymmdd"));
        delParams.put("ser", params.get("oldser"));
        pdPlanMapper.deletePdPlan(delParams);

        // 3. 신규 순번 채번 및 재등록
        Map<String, Object> serParams = new HashMap<>();
        serParams.put("cmpycd", oldPlan.getCmpycd());
        serParams.put("yymmdd", params.get("newyymmdd").toString());
        String newSer = pdPlanMapper.selectNextSer(serParams);

        oldPlan.setYymmdd(params.get("newyymmdd").toString());
        oldPlan.setSer(newSer);
        
        String updemp = params.get("updemp").toString();
        if (updemp.length() > 20) updemp = updemp.substring(0, 20);
        oldPlan.setUpdemp(updemp);
        
        // 🚀 [해결] 자동 전계(insertPdPlan)가 아닌 단건 저장(insertPdPlanSingle) 호출
        pdPlanMapper.insertPdPlanSingle(oldPlan);
        log.info("🚚 계획 일자 이동 완료: {} -> {}", params.get("oldyymmdd"), params.get("newyymmdd"));
    }
}
