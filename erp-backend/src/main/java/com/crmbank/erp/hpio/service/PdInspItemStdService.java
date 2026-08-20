package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspItemStdDto;
import com.crmbank.erp.hpio.mapper.PdInspItemStdMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspItemStdService {

    private final PdInspItemStdMapper pdInspItemStdMapper;

    /**
     * 품목별 검사항목 목록 조회
     * 💡 데이터가 없으면 공통코드(830)에서 기본 항목을 조회함
     */
    public List<PdInspItemStdDto> getPdInspItemStdList(Map<String, Object> params) {
        int count = pdInspItemStdMapper.selectPdInspItemStdCnt(params);
        params.put("exists", count > 0);
        return pdInspItemStdMapper.getPdInspItemStdList(params);
    }

    /**
     * 품목별 검사항목 저장 (등록/수정/삭제 통합)
     */
    @Transactional
    public void savePdInspItemStd(List<PdInspItemStdDto> dtoList, String cmpycd, String userId) {
        for (PdInspItemStdDto dto : dtoList) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                pdInspItemStdMapper.insertPdInspItemStd(dto);
            } else if ("U".equals(dto.getState())) {
                pdInspItemStdMapper.updatePdInspItemStd(dto);
            } else if ("D".equals(dto.getState())) {
                pdInspItemStdMapper.deletePdInspItemStd(Map.of(
                        "cmpycd", cmpycd,
                        "itemcd", dto.getItemcd(),
                        "insp_gb", dto.getInsp_gb(),
                        "inspcd", dto.getInspcd()
                ));
            }
        }
    }
}
