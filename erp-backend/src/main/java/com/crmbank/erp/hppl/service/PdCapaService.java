package com.crmbank.erp.hppl.service;

import com.crmbank.erp.hppl.dto.PdCapaDto;
import com.crmbank.erp.hppl.mapper.PdCapaMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdCapaService {

    private final PdCapaMapper pdCapaMapper;

    public List<Map<String, Object>> getProductItemList(Map<String, Object> params) {
        return pdCapaMapper.getProductItemList(params);
    }

    public List<PdCapaDto> getProductCapaList(Map<String, Object> params) {
        return pdCapaMapper.getProductCapaList(params);
    }

    @Transactional
    public void saveProductCapa(List<PdCapaDto> list, String cmpycd, String userId) {
        for (PdCapaDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            // 🚀 [교정] 일생산량(pqtydd) = (가동시간 / 개당소요시간(h)) * 가동율 * 양품율
            BigDecimal capahh = dto.getCapahh() != null && dto.getCapahh().compareTo(BigDecimal.ZERO) > 0 ? dto.getCapahh() : null;
            BigDecimal gadtmdd = dto.getGadtmdd() != null ? dto.getGadtmdd() : BigDecimal.ZERO;
            BigDecimal gadrate = dto.getGadrate() != null ? dto.getGadrate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;
            BigDecimal jungrate = dto.getJungrate() != null ? dto.getJungrate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;

            if (capahh != null) {
                // (가동시간 / 소요시간) * 가동율 * 양품율
                BigDecimal pqtydd = gadtmdd.divide(capahh, 10, RoundingMode.HALF_UP)
                                           .multiply(gadrate)
                                           .multiply(jungrate)
                                           .setScale(0, RoundingMode.FLOOR); // 생산량은 소수점 버림
                dto.setPqtydd(pqtydd);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            params.put("itemcd", dto.getItemcd());
            params.put("linecd", dto.getLinecd());
            params.put("progcd", dto.getProgcd());

            if (pdCapaMapper.selectPdCapaCnt(params) > 0) {
                pdCapaMapper.updatePdCapa(dto);
            } else {
                pdCapaMapper.insertPdCapa(dto);
            }
        }
    }

    @Transactional
    public void deleteProductCapa(List<PdCapaDto> list, String cmpycd) {
        for (PdCapaDto dto : list) {
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            params.put("itemcd", dto.getItemcd());
            params.put("linecd", dto.getLinecd());
            params.put("progcd", dto.getProgcd());
            pdCapaMapper.deletePdCapa(params);
        }
    }
}
