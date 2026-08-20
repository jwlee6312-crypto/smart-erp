package com.crmbank.erp.crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.handler.CtiWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CtiWebSocketHandler ctiWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ctiWebSocketHandler, "/ws/cti")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        
                        // 💡 [진단 로그] 브라우저로부터 들어온 모든 신호를 가감 없이 출력합니다.
                        String query = request.getURI().getQuery();
                        log.info("📡 [CTI WS] 연결 시도 감지 - URI: {}, Remote: {}", request.getURI(), request.getRemoteAddress());

                        if (query != null && query.contains("exten=")) {
                            try {
                                String exten = query.split("exten=")[1].split("&")[0];
                                attributes.put("inner_no", exten);
                                log.info("🎯 [인증 성공] 내선번호 [{}] 소켓 세션 매핑 완료", exten);
                            } catch (Exception e) {
                                log.error("❌ [인증 실패] 쿼리 파싱 에러: {}", e.getMessage());
                            }
                        } else {
                            log.warn("⚠️ [경고] 내선번호(exten) 정보가 요청에 포함되지 않았습니다.");
                        }

                        return super.beforeHandshake(request, response, wsHandler, attributes);
                    }
                })
                .setAllowedOrigins("*");
    }
}
