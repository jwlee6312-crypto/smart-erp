<!--
	=============================================================
	프로그램명	  : SmartCore 통합 오퍼레이션 콕핏 (최종 완결본)
    프로그램 ID	: DEFAULT_PAGE
	작성일자	    : 25.03.14
	작성자	      : AI Assistant
	설명         : 시계 추가 + 실적 지표(HGOA200U 디자인) + 원본 위젯 전체 복구
	=============================================================
-->

<template>
    <div class="cockpit-wrapper bg-soft-gray h-100 overflow-auto p-3 text-start">
        <!-- [1] 상단 통합 관제 헤더 -->
        <header class="d-flex justify-content-between align-items-center mb-2 border-bottom pb-2">
            <div class="d-flex align-items-center gap-3">
                <h6 class="fw-bold text-dark mb-0">
                    <i class="bi bi-grid-1x2-fill text-primary me-2"></i>OPERATION COCKPIT
                </h6>
                <div class="vr opacity-25" style="height: 15px;"></div>
                <p class="text-muted extra-small mb-0">실시간 전사 운영 지표 통합 모니터링</p>
            </div>
            <!-- 🕒 실시간 디지털 시계 -->
            <div class="digital-clock d-flex align-items-center gap-2 px-3 py-1 bg-white rounded-3 shadow-xs border">
                <i class="bi bi-clock-fill text-primary small"></i>
                <span class="fw-bold text-primary font-monospace" style="font-size: 1rem;">{{ currentTime }}</span>
            </div>
        </header>

        <!-- [2] 리얼타임 피드 -->
        <div class="live-feed-ribbon mb-3 p-2 bg-light rounded-3 border shadow-xs overflow-hidden">
            <div class="d-flex align-items-center gap-3">
                <span class="badge bg-danger rounded-pill px-2 py-1 fw-bold extra-small pulse-border">LIVE FEED</span>
                <div class="flex-grow-1 extra-small text-dark fw-bold overflow-hidden" style="position: relative; height: 20px;">
                    <div class="scrolling-text">
                        [시스템] 실시간 인바운드 상담 엔진 가동 중 --- [알림] 금주 약정 만료 건 확인 필요 --- [영업] 당일 상담 목표 달성률 92% --- [알림] 시스템 정기 점검 예정
                    </div>
                </div>
            </div>
        </div>

        <!-- [3] 4대 핵심 영역 요약 -->
        <div class="row g-2 mb-3">
            <div class="col-md-3" v-for="stat in quickStats" :key="stat.label">
                <div class="card border-0 shadow-sm rounded-3 bg-white border-start border-4 py-1" :class="'border-' + stat.color">
                    <div class="card-body py-1 px-3">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <div class="text-muted extra-small fw-bold">{{ stat.label }}</div>
                                <div class="fw-bold text-dark" style="font-size: 1.1rem;">{{ stat.value }}<span class="extra-small fw-normal ms-1 opacity-50">건</span></div>
                            </div>
                            <div class="stat-icon-area opacity-25">
                                <i :class="'bi bi-' + stat.icon" class="fs-4"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- [4] 메인 관제 위젯 그리드 -->
        <div class="row g-3">
            <!-- (1) CRM실적 모니터링 (요청하신 녹색 테마 + HGOA200U 디자인 이식) -->
            <div class="col-md-7 d-flex flex-column gap-3">
                <div class="card border-0 shadow-sm rounded-4 overflow-hidden bg-white h-100">
                    <div class="card-header bg-success py-2 px-3 d-flex justify-content-between align-items-center border-0">
                        <span class="fw-bold text-white small"><i class="bi bi-bar-chart-line-fill me-2"></i>CRM실적 모니터링</span>
                        <button class="btn btn-xs btn-outline-light px-2 py-0 fw-bold" @click="$router.push('/HGO/HGOA200U')">자세히보기</button>
                    </div>
                    <div class="card-body p-3">
                        <!-- 지표 영역 (HGOA200U 원본 디자인) -->
                        <div class="row g-2 mb-3">
                            <div class="col-6">
                                <div class="card border-0 shadow-sm border-top border-3 border-warning rounded-3 bg-white h-100">
                                    <div class="card-header bg-white py-1 px-3 fw-bold extra-small d-flex justify-content-between border-0">
                                        <span>오늘의 인바운드 현황</span>
                                        <span class="text-muted">Live</span>
                                    </div>
                                    <div class="card-body p-2 d-flex justify-content-around text-center align-items-center">
                                        <div><div class="text-muted extra-small">인입</div><div class="fw-bold small">{{ metrics.inboundTotal }}</div></div>
                                        <div class="vr opacity-25" style="height: 15px;"></div>
                                        <div><div class="text-success extra-small">응대</div><div class="fw-bold small">{{ metrics.inboundAnswered }}</div></div>
                                        <div class="vr opacity-25" style="height: 15px;"></div>
                                        <div><div class="text-danger extra-small">포기</div><div class="fw-bold small">{{ metrics.inboundAbandoned }}</div></div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-6">
                                <div class="card border-0 shadow-sm border-top border-3 border-success rounded-3 bg-white h-100">
                                    <div class="card-header bg-white py-1 px-3 fw-bold extra-small d-flex justify-content-between border-0">
                                        <span>아웃바운드 캠페인 실적</span>
                                        <span class="text-success">Perf.</span>
                                    </div>
                                    <div class="card-body p-2 d-flex justify-content-around text-center align-items-center">
                                        <div><div class="text-muted extra-small">지시</div><div class="fw-bold small">{{ metrics.outboundTotal }}</div></div>
                                        <div class="vr opacity-25" style="height: 15px;"></div>
                                        <div><div class="text-primary extra-small">성공</div><div class="fw-bold small">{{ metrics.outboundSuccess }}</div></div>
                                        <div class="vr opacity-25" style="height: 15px;"></div>
                                        <div><div class="text-dark extra-small">성공률</div><div class="fw-bold small">{{ metrics.outboundRate }}%</div></div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 콜백/미결 샘플 리스트 -->
                        <div class="mt-3">
                            <div class="extra-small fw-bold text-secondary mb-2 ms-1"><i class="bi bi-list-stars me-1 text-primary"></i>실시간 미결/콜백 대기 (샘플)</div>
                            <div class="list-group list-group-flush border rounded-3 overflow-hidden shadow-xs">
                                <div v-for="item in crmList" :key="item.id" class="list-group-item d-flex justify-content-between align-items-center py-2 border-light">
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="status-dot" :class="item.type === '콜백요청' ? 'bg-danger' : 'bg-warning'"></div>
                                        <span class="fw-bold text-dark extra-small">{{ item.phone }}</span>
                                        <span class="extra-small text-muted opacity-75">{{ item.time }}</span>
                                    </div>
                                    <span class="extra-small badge bg-light text-dark border fw-normal">{{ item.type }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 전사 공지사항 (복구) -->
                <div class="card border-0 shadow-sm rounded-4 border-top border-4 border-dark bg-white mt-3">
                    <div class="card-header bg-white py-2 px-3 d-flex justify-content-between align-items-center border-bottom">
                        <span class="fw-bold text-dark small"><i class="bi bi-megaphone me-2 text-dark"></i>전사 공지사항</span>
                    </div>
                    <div class="card-body p-0">
                        <div class="list-group list-group-flush">
                            <div v-for="notice in noticeList" :key="notice.id" class="list-group-item d-flex justify-content-between align-items-center py-2 border-light">
                                <span class="extra-small text-dark text-truncate px-3" style="max-width: 400px;">{{ notice.title }}</span>
                                <span class="extra-small text-muted pe-3">{{ notice.date }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- (2) 구매/영업/약정 관제 (우측 복구) -->
            <div class="col-md-5 d-flex flex-column gap-3">
                <div class="card border-0 shadow-sm rounded-4 border-top border-4 border-warning bg-white">
                    <div class="card-header bg-white py-2 px-3 border-bottom">
                        <span class="fw-bold text-dark small"><i class="bi bi-box-seam me-2 text-warning"></i>구매 및 입고 미결</span>
                    </div>
                    <div class="card-body p-3">
                        <div class="row g-2 text-center">
                            <div class="col-6 border-end">
                                <div class="extra-small text-muted fw-bold">발주 미입고</div>
                                <div class="fw-bold text-warning fs-5">5<span class="extra-small fw-normal ms-1 opacity-50">건</span></div>
                            </div>
                            <div class="col-6">
                                <div class="extra-small text-muted fw-bold">입고 미정산</div>
                                <div class="fw-bold text-dark fs-5">7<span class="extra-small fw-normal ms-1 opacity-50">건</span></div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card border-0 shadow-sm rounded-4 border-top border-4 border-success bg-white">
                    <div class="card-header bg-white py-2 px-3 border-bottom">
                        <span class="fw-bold text-dark small"><i class="bi bi-cart-check me-2 text-success"></i>영업 및 채권 미결</span>
                    </div>
                    <div class="card-body p-3">
                        <div class="row g-2 text-center">
                            <div class="col-4 border-end"><div class="extra-small text-muted fw-bold">주문미출고</div><div class="fw-bold text-dark fs-6">12</div></div>
                            <div class="col-4 border-end"><div class="extra-small text-muted fw-bold">출고미정산</div><div class="fw-bold text-dark fs-6">8</div></div>
                            <div class="col-4"><div class="extra-small text-danger fw-bold">미입금</div><div class="fw-bold text-danger fs-6">5</div></div>
                        </div>
                    </div>
                </div>

                <div class="card border-0 shadow-sm rounded-4 border-top border-4 border-primary bg-white">
                    <div class="card-header bg-white py-2 px-3 d-flex justify-content-between align-items-center border-bottom">
                        <span class="fw-bold text-dark small"><i class="bi bi-clock-history me-2 text-primary"></i>금주 약정만료 현황</span>
                    </div>
                    <div class="card-body p-0">
                        <table class="table table-sm extra-small mb-0">
                            <thead class="bg-light text-muted">
                                <tr><th class="ps-3">거래처명</th><th>만료일</th><th class="text-end pe-3">금액</th></tr>
                            </thead>
                            <tbody>
                                <tr v-for="item in expireList" :key="item.cust" class="border-light">
                                    <td class="ps-3 py-2 fw-bold text-dark text-truncate" style="max-width: 150px;">{{ item.cust }}</td>
                                    <td class="text-danger fw-bold">{{ item.date }}</td>
                                    <td class="text-end pe-3 fw-bold">₩ {{ item.amt }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

const currentTime = ref('')
const updateClock = () => {
    currentTime.value = new Date().toLocaleTimeString('ko-KR', { hour12: false });
}

const metrics = reactive({
    inboundTotal: 45, inboundAnswered: 38, inboundAbandoned: 7,
    outboundTotal: 120, outboundSuccess: 82, outboundRate: 68
})

const quickStats = ref([
    { label: '구매 미결', value: 12, icon: 'box-seam', color: 'warning' },
    { label: '영업 미결', value: 25, icon: 'cart-check', color: 'success' },
    { label: '미결 응대', value: 8, icon: 'headset', color: 'danger' },
    { label: '약정 만료', value: 3, icon: 'clock-history', color: 'primary' }
])

const crmList = ref([
    { id: 1, type: '콜백요청', phone: '010-1234-5678', time: '14:20' },
    { id: 2, type: '부재중', phone: '02-5566-7788', time: '15:05' },
    { id: 3, type: '콜백요청', phone: '010-9988-7766', time: '16:30' }
]);

const noticeList = ref([
    { id: 1, title: '시스템 정기 점검 안내 (3월 15일)', date: '2025-03-13' },
    { id: 2, title: '개인정보 취급방침 개정 공지', date: '2025-03-12' }
]);

const expireList = ref([
    { cust: '(주)하이텍네트웍스', date: '03-15', amt: '1.2M' },
    { cust: '대성정밀시스템', date: '03-17', amt: '0.8M' },
    { cust: '글로벌소프트(주)', date: '03-20', amt: '2.5M' }
]);

onMounted(() => {
    updateClock();
    setInterval(updateClock, 1000);
})
</script>

<style scoped>
.cockpit-wrapper { height: calc(100vh - 65px); font-family: 'Inter', sans-serif; }
.bg-soft-gray { background-color: #f1f5f9; }
.extra-small { font-size: 0.72rem; }
.mini-text { font-size: 0.65rem; color: #94a3b8; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.scrolling-text {
    position: absolute; width: 100%; height: 100%; white-space: nowrap;
    animation: scroll-left 35s linear infinite;
}
@keyframes scroll-left { 0% { transform: translateX(100%); } 100% { transform: translateX(-200%); } }
.pulse-border { animation: pulse-soft 2s infinite; }
@keyframes pulse-soft {
    0% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0.4); }
    70% { box-shadow: 0 0 0 6px rgba(220, 53, 69, 0); }
    100% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0); }
}
</style>
