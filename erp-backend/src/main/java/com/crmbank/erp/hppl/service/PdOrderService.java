package com.crmbank.erp.hppl.service;

import com.crmbank.erp.hppl.dto.PdOrderDto;
import com.crmbank.erp.hppl.mapper.PdOrderMapper;
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
public class PdOrderService {

    private final PdOrderMapper pdOrderMapper;

    public List<PdOrderDto> getPdOrderTargetList(Map<String, Object> params) {
        return pdOrderMapper.getPdOrderTargetList(params);
    }

    public List<PdOrderDto> getPdOrderList(Map<String, Object> params) {
        return pdOrderMapper.getPdOrderList(params);
    }

    /**
     * 작업지시 일괄 저장
     */
    @Transactional
    public void savePdOrderList(List<PdOrderDto> list, String cmpycd, String userId) {
        String safeUserId = (userId != null && userId.length() > 10) ? userId.substring(0, 10) : userId;

        for (PdOrderDto dto : list) {
            String status = dto.get_status();
            if (status == null || status.isEmpty()) continue;

            dto.setCmpycd(cmpycd);
            dto.setUpdemp(safeUserId);
            
            // 날짜 가공 (하이픈 제거)
            if (dto.getOrdymd() != null) dto.setOrdymd(dto.getOrdymd().replace("-", ""));
            if (dto.getLotymd() != null) dto.setLotymd(dto.getLotymd().replace("-", ""));
            if (dto.getYymmdd() != null) dto.setYymmdd(dto.getYymmdd().replace("-", ""));

            // 지시년월(ORDYM) 6자리 제한
            if (dto.getOrdym() != null) {
                String ym = dto.getOrdym().replace("-", "");
                dto.setOrdym(ym.length() > 6 ? ym.substring(0, 6) : ym);
            } else {
                dto.setOrdym(dto.getOrdymd() != null && dto.getOrdymd().length() >= 6 ? dto.getOrdymd().substring(0, 6) : "000000");
            }

            // 주문번호(ORDNO) 4자리 엄격 체크
            if (dto.getOrdno() == null || dto.getOrdno().trim().isEmpty()) {
                dto.setOrdno("0000");
            } else if (dto.getOrdno().length() > 4) {
                throw new RuntimeException("주문번호(ORDNO)는 4자리를 초과할 수 없습니다: " + dto.getOrdno());
            }

            // 숫자형 필드 NULL 방지
            if (dto.getOrdqty() == null) dto.setOrdqty(java.math.BigDecimal.ZERO);
            if (dto.getLotsize() == null) dto.setLotsize(java.math.BigDecimal.ZERO);
            if (dto.getProdqty() == null) dto.setProdqty(java.math.BigDecimal.ZERO);
            if (dto.getPlanqty() == null) dto.setPlanqty(java.math.BigDecimal.ZERO);
            
            // 비고(BIGO) 컬럼 크기 제한
            if (dto.getCustnm() != null && dto.getCustnm().length() > 50) {
                dto.setCustnm(dto.getCustnm().substring(0, 50));
            } else if (dto.getCustnm() == null) {
                dto.setCustnm("");
            }

            try {
                if ("입력".equals(status)) {
                    // 1. 신규 일련번호(SEQ) 3자리 채번
                    Map<String, Object> lotParams = new HashMap<>();
                    lotParams.put("cmpycd", cmpycd);
                    lotParams.put("linecd", dto.getLinecd());
                    lotParams.put("itemcd", dto.getItemcd());
                    lotParams.put("ordymd", dto.getOrdymd());
                    
                    String newSeq = pdOrderMapper.selectNewLotNo(lotParams);
                    dto.setLotno(newSeq); // 🚀 [핵심수정] 3자리 일련번호 그대로 사용 (L+날짜 접두사 제거)

                    // 🚀 [DEBUG] 저장 전 로그
                    log.info("🚀 [HPIO200T INSERT] lotno:({}), ordym:{}, ordno:{}", dto.getLotno(), dto.getOrdym(), dto.getOrdno());

                    // 2. 마스터 및 상세 저장
                    pdOrderMapper.insertPdLotNo(dto);
                    pdOrderMapper.insertPdOrder(dto);
                    
                    // 3. 생산계획 테이블의 지시수량 업데이트
                    updatePlanOrdQty(dto, dto.getOrdqty());
                } 
                else if ("수정".equals(status)) {
                    pdOrderMapper.updatePdLotNo(dto);
                    pdOrderMapper.updatePdOrder(dto);
                } 
                else if ("삭제".equals(status)) {
                    updatePlanOrdQty(dto, dto.getOrdqty().negate());
                    pdOrderMapper.deletePdLotNo(dto);
                    pdOrderMapper.deletePdOrder(dto);
                }
            } catch (Exception e) {
                log.error("❌ 작업지시 처리 오류: {}", e.getMessage());
                throw new RuntimeException("작업지시 저장 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    private void updatePlanOrdQty(PdOrderDto dto, java.math.BigDecimal qty) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", dto.getCmpycd());
        params.put("yymmdd", dto.getYymmdd());
        params.put("ser", dto.getSer());
        params.put("itemcd", dto.getItemcd());
        params.put("qty", qty);
        pdOrderMapper.updatePlanOrdQty(params);
    }
}
