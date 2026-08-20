<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useManualStore } from '@/stores/manualStore'
import { useAuthStore } from '@/stores/authStore'
import { useCtiStore } from '@/stores/ctiStore'
import ManualPopup from '@/layouts/ManualLayout.vue'

const popup = useManualStore()
const authStore = useAuthStore()
const ctiStore = useCtiStore()

onMounted(() => {
	// 브라우저 알림 권한 요청
	if (Notification.permission === 'default') {
		Notification.requestPermission()
	}

	// 💡 초기 진입 시 이미 로그인 상태라면 CTI 연결
	if (authStore.isAuthenticated) {
		ctiStore.connect()
	}
})

// 💡 로그인 상태 변화 감시 (로그인 성공 시 즉시 CTI 연결)
watch(() => authStore.isAuthenticated, (isAuth) => {
	if (isAuth) {
		ctiStore.connect()
	} else {
		ctiStore.disconnect()
	}
})
</script>

<template>
	<router-view />

	<teleport to="body">
		<transition name="slide-fade">
			<div v-if="popup.isOpen" class="manual-floating-panel shadow-lg">
				<ManualPopup :fileName="popup.fileName" @close="popup.close()" />
			</div>
		</transition>
	</teleport>
</template>

<style>
/* 도움말 전용 우측 완전 고정 사이드바 - 탭 레이아웃 간섭 무시 */
.manual-floating-panel {
	position: fixed !important;
	top: 0 !important;
	right: 0 !important;
	bottom: 0 !important;
	left: auto !important; /* 왼쪽 고정 해제 */
	width: 350px !important; /* 가독성을 위해 너비 약 10% 확대 */
	height: 100vh !important;
	background: #fff !important;
	z-index: 999999 !important; /* 무조건 최상위 */
	box-shadow: -5px 0 20px rgba(0, 0, 0, 0.15) !important;
	border-left: 1px solid #c8ced3 !important;
	display: flex !important;
	flex-direction: column !important;
}

/* 애니메이션: 우측 끝에서 튀어나옴 */
.slide-fade-enter-active, .slide-fade-leave-active {
	transition: transform 0.2s ease-in-out !important;
}
.slide-fade-enter-from, .slide-fade-leave-to {
	transform: translateX(100%) !important;
}
</style>
