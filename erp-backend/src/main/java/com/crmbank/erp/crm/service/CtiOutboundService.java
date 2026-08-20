package com.crmbank.erp.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.action.OriginateAction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CtiOutboundService {

    private final ManagerConnection managerConnection;

    /**
     * 💡 아웃바운드 발신 실행 및 생성된 파일명 반환
     */
    public String makeCall(String exten, String dest, String context) {
        final String targetContext = (context == null || context.isEmpty()) ? "haion-outbound" : context;
        
        // 1. 백엔드에서 표준 파일명 생성
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String recFileName = String.format("%s-%s-%s", timestamp, exten, dest);

        new Thread(() -> {
            try {
                OriginateAction action = new OriginateAction();
                action.setChannel("PJSIP/" + exten); 
                action.setContext(targetContext);    
                action.setExten(dest);               
                action.setPriority(1);
                action.setCallerId("CRM <" + exten + ">");
                action.setTimeout(30000L);
                action.setAsync(true);

                // 2. Asterisk 다이얼플랜으로 녹취파일명 전달
                Map<String, String> variables = new HashMap<>();
                variables.put("REC_FILE", recFileName);
                action.setVariables(variables);

                if (managerConnection.getState().toString().equals("CONNECTED")) {
                    managerConnection.sendAction(action);
                    log.info("🚀 [OUTBOUND] 발신 요청: {} -> {} [FILE:{}]", exten, dest, recFileName);
                }
            } catch (Exception ex) {
                log.error("❌ [OUTBOUND] 오류: {}", ex.getMessage());
            }
        }).start();

        return recFileName + ".wav"; // 💡 확장자 포함 반환
    }

    public String makeCall(String exten, String dest) {
        return makeCall(exten, dest, "haion-outbound");
    }
}
