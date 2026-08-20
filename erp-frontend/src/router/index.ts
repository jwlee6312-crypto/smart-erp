import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '@/views/LoginPage.vue'
import { useAuthStore } from '@/stores/authStore.ts'
import NotFound from '@/views/NotFound.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import EmptyLayout from '@/layouts/EmptyLayout.vue'
import ProfilePages from '@/views/ProfilePages.vue'
import { useSession } from '@/composables/useSession'
import { nextTick } from 'vue'
import { activation } from '@/utils/menuActivation'
import { checkVer } from '@/composables/useVersionCheck'

const routes = [
	{
		path: '/',
		name: 'DefaultLayout',
		component: DefaultLayout,
		children: [
            { path: '', name: 'Home', component: () => import('@/views/DefaultPage.vue') },
			{ path: '/profile', name: '프로필수정', component: ProfilePages },

            // HGOA (캠페인/상담관리)
            {
                path: '/HGOA/HGOA100U',
                name: '옴니채널상담원',
                component: () => import('@/views/HGOA/HGOA100U.vue'),
                meta: { topMenuCode: 'CRM', sideMenuCode: 'HGOA100U', title: '옴니채널상담원' }
            },
            {
                path: '/HSIO600U',
                name: 'HSIO600U',
                component: () => import('@/views/HSIO/HSIO600U.vue')
            },
		],
	},
    // 💡 고객용 상담실 (로그인 세션 없이 접근 가능)
    {
        path: '/HGOA/HGOA100C',
        name: '고객상담실',
        component: () => import('@/views/HGOA/HGOA100C.vue')
    },
	{
		path: '/auth',
		component: EmptyLayout,
		children: [{ path: 'login', name: 'Login', component: LoginPage }],
	},
    // 📱 모바일 전용 메인 런처 추가
    {
        path: '/mobile',
        name: 'MobileMain',
        component: () => import('@/views/MobileMain.vue'),
        meta: { title: '모바일 메인' }
    },
	{ path: '/manual:fileName', component: () => import('@/layouts/ManualLayout.vue'), props: true },
	{ path: '/:pathMatch(.*)*', component: NotFound },
]

const router = createRouter({ history: createWebHistory(), routes })

// 💡 직접 접속 시에도 튕기지 않도록 패턴 허용 (H로 시작하는 업무 페이지 허용)
router.beforeEach(async (to, from, next) => {
	const authStore = useAuthStore();
    const { checkSession } = useSession()

	// 1. 버전 체크
	const versionOk = await checkVer()
	if (!versionOk) {
        alert('버전이 업데이트 되었습니다.');
        await authStore.logout();
        return next('/auth/login')
    }

	// 2. 화이트리스트 및 첫 진입 체크 (직접 주소 입력 및 새로고침 대응)
    // H로 시작하는 8자리 프로그램 ID 패턴이면 세션 체크만 하고 통과시킵니다.
    const isBusinessPage = /^\/[A-Z]{4}\d{3}[A-Z]$/.test(to.path) || to.path.includes('/HGOA');

	if (!from.matched.length && !isBusinessPage && !['/', '/auth/login', '/manual', '/HGOA/HGOA100C'].includes(to.path)) {
        return next('/')
    }

	if (to.path === '/auth/login') {
        const active = await checkSession();
        return active ? next('/') : next()
    }

    // 3. 세션 체크
	const active = await checkSession()
	if (active) return next()

	authStore.resetState()
    return next('/auth/login')
})

router.afterEach(async (to) => {
	await nextTick()
    // 4. 메뉴 활성화 로직 복구
	const { topMenuCode, sideMenuCode, title } = to.meta
	if (topMenuCode && sideMenuCode && title) {
        await activation(topMenuCode as string, sideMenuCode as string, title as string)
    }
})

export default router
