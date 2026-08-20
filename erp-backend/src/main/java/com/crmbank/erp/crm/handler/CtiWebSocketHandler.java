package com.crmbank.erp.crm.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class CtiWebSocketHandler extends TextWebSocketHandler {

    // 💡 내선번호당 여러 개의 세션(메인 창, 팝업 창 등)을 허용하도록 수정
    private static final Map<String, List<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String exten = getExtension(session);

        if (exten != null && !exten.isEmpty()) {
            SESSIONS.computeIfAbsent(exten, k -> new CopyOnWriteArrayList<>()).add(session);
            log.info("🎯 [CTI WS] 상담원 연결 완료: 내선번호 [{}], 총 세션 수: {}", exten, SESSIONS.get(exten).size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String exten = getExtension(session);
        if (exten != null) {
            List<WebSocketSession> sessions = SESSIONS.get(exten);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    SESSIONS.remove(exten);
                }
                log.info("🏠 [CTI WS] 상담원 세션 종료: [{}], 남은 세션 수: {}", exten, sessions.size());
            }
        }
    }

    private String getExtension(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String exten = (String) attributes.get("inner_no");
        if (exten == null) {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("exten=")) {
                exten = query.split("exten=")[1].split("&")[0];
            }
        }
        return exten;
    }

    public boolean isSessionActive(String exten) {
        List<WebSocketSession> sessions = SESSIONS.get(exten);
        return sessions != null && !sessions.isEmpty() && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    public List<String> getActiveExtens() {
        return new ArrayList<>(SESSIONS.keySet());
    }

    public void sendMessage(String exten, String message) {
        List<WebSocketSession> sessions = SESSIONS.get(exten);
        if (sessions != null) {
            // 💡 해당 내선번호로 연결된 모든 창(메인, 팝업 등)에 메시지 브로드캐스트
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("❌ [CTI WS] 전송 실패 (내선: {}): {}", exten, e.getMessage());
                    }
                }
            }
        }
    }
}
