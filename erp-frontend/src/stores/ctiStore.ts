import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/authStore'

export const useCtiStore = defineStore('cti', () => {
	const authStore = useAuthStore()
	const isConnected = ref(false)
	const incomingCall = ref<any>(null)

	const isDrawerOpen = ref(false)
	const isTalking = ref(false)
	const recordingFile = ref('') // 💡 녹취 파일명 저장용
	
	let socket: WebSocket | null = null
	let ringtoneAudio: HTMLAudioElement | null = null

	const connect = () => {
		const targetExten = authStore.extension;
		if (!targetExten) return;
		if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;
		
		const wsUrl = `ws://${window.location.hostname}:8080/ws/cti?exten=${targetExten}`;
		socket = new WebSocket(wsUrl)

		socket.onopen = () => { isConnected.value = true; }

		socket.onmessage = (event) => {
			const data = JSON.parse(event.data)
			console.log('📢 [CTI 신호 수신]:', data)

			if (data.type === 'INBOUND_CALL') {
				incomingCall.value = data
				isDrawerOpen.value = true
				isTalking.value = false
				recordingFile.value = ''
				playRingtone()
			} else if (data.type === 'CALL_CONNECTED') {
				isTalking.value = true
				stopRingtone()
			} else if (data.type === 'CALL_HANGUP') {
				isTalking.value = false
				stopRingtone()
				if (data.recordingFile) {
					recordingFile.value = data.recordingFile
				}
			}
		}

		socket.onclose = () => {
			isConnected.value = false
			if (authStore.isAuthenticated && authStore.extension) {
				setTimeout(connect, 5000)
			}
		}
	}

	const disconnect = () => {
		if (socket) { socket.close(); socket = null; }
		isConnected.value = false;
		stopRingtone();
	}

	watch(() => authStore.extension, (newExt) => {
		if (newExt && !isConnected.value) connect();
	}, { immediate: true });

	const closeDrawer = () => {
		isDrawerOpen.value = false
		incomingCall.value = null
		isTalking.value = false
		stopRingtone()
	}

	const playRingtone = () => {
		if (!ringtoneAudio) {
			ringtoneAudio = new Audio('https://assets.mixkit.co/active_storage/sfx/2358/2358-preview.mp3')
			ringtoneAudio.loop = true
		}
		ringtoneAudio.play().catch(() => {})
	}

	const stopRingtone = () => {
		if (ringtoneAudio) { ringtoneAudio.pause(); ringtoneAudio.currentTime = 0; }
	}

	return { isConnected, incomingCall, isDrawerOpen, isTalking, recordingFile, connect, disconnect, closeDrawer, stopRingtone }
})
