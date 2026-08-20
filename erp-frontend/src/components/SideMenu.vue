<template>
  <div class="side-bar shadow-sm" :class="{ 'is-collapsed': isCollapsed }">
    <!-- 유저 프로필 카드 (영역 압축 및 사진 확대 최종 버전) -->
    <div v-if="!isCollapsed" class="profile-card">
      <div class="user-details text-center">
        <div class="avatar-area mb-2">
          <div class="avatar-placeholder cursor-pointer" @click="goToProfile" title="개인정보 관리로 이동">
            <img v-if="profileImageSrc" :src="profileImageSrc" class="profile-img" alt="Profile" />
            <i v-else class="bi bi-person-fill"></i>
          </div>
        </div>
        <div class="user-info-text">
          <div class="user-name-info">
            {{ authStore.usernm }}(내선:{{ authStore.inner_no || '-' }})
          </div>
          <div v-if="authStore.email" class="user-email-text">
            메일:{{ authStore.email }}
          </div>
        </div>
      </div>
    </div>

    <!-- 메뉴 리스트 -->
    <div id="accordionMenu" class="menu-list">
      <div v-for="group in groupedItems" :key="group.grpcd" class="w-100">
        <a
          class="nav-link group-title"
          :class="{ 'collapsed': !isGroupOpen(group.grpcd) }"
          href="javascript:void(0)"
          @click="toggleGroup(group.grpcd)"
        >
          <i class="bi bi-folder2-open me-2"></i>
          <span v-if="!isCollapsed">{{ group.grpnm }}</span>
        </a>

        <div class="collapse-content" v-show="isGroupOpen(group.grpcd) && !isCollapsed">
          <nav class="nav d-flex flex-column">
            <a
              v-for="item in group.items"
              :key="item.pgmid"
              class="sb-nav-link"
              :class="{ 'is-active': tabStore.activeTabId === item.pgmid }"
              href="javascript:void(0)"
              :title="item.pgmid"
              @click="goPage(item.pgmid, item.pgmnm, item.grpcd)"
            >
              <i class="bi bi-chevron-right sub-icon"></i>
              <span>{{ item.pgmnm }}</span>
            </a>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMenuStore } from '@/stores/menuStore'
import { useTabStore } from '@/stores/tabStore'
import { useAuthStore } from '@/stores/authStore'
import { addDynamicRoute } from '@/router/dynamicRoute'
import { API_URL } from '@/config/api'

const props = defineProps({
  isCollapsed: Boolean
})

const authStore = useAuthStore()
const menuStore = useMenuStore()
const tabStore = useTabStore()
const openGroupId = ref<string | null>(null)

// 🚀 프로필 이미지 경로 계산
const profileImageSrc = computed(() => {
  if (authStore.photo_path) {
    const path = authStore.photo_path.trim()
    if (path.startsWith('http') || path.startsWith('data:')) return path

    // 💡 [보정] 정책 변경 반영: /Upload_Images/{cmpycd}/profile/{filename}
    // DB에는 파일명만 저장되어 있으므로 경로를 조합합니다.
    const cmpycd = authStore.cmpycd || 'coit'
    return `/Upload_Images/${cmpycd}/profile/${path}`
  }
  return ''
})

const isGroupOpen = (grpcd: string) => openGroupId.value === grpcd
const toggleGroup = (grpcd: string) => {
  openGroupId.value = openGroupId.value === grpcd ? null : grpcd
}

const groupedItems = computed(() => menuStore.groupedSidebarItems)

function goToProfile() {
  const pgmId = 'HABA910U';
  const pgmNm = '개인정보 관리';
  const grpCd = '900'; // 시스템 관리 그룹 코드

  // 🚀 공식 메뉴 이동 로직과 동일하게 처리 (탭 추가 및 이동)
  addDynamicRoute(pgmId, pgmNm, grpCd);
  tabStore.addTab({ pgmId: pgmId, pgmNm: pgmNm, path: `/${pgmId}` });
}

function goPage(pgmid: string, pgmnm: string, grpcd: string) {
  addDynamicRoute(pgmid, pgmnm, grpcd)
  tabStore.addTab({ pgmId: pgmid, pgmNm: pgmnm, path: `/${pgmid}` })
}
</script>

<style scoped>
.side-bar { background-color: #fff; border-right: 1px solid #dcdfe6; display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.profile-card { padding: 15px 10px; border-bottom: 1px solid #ebeef5; flex-shrink: 0; background: #f8f9fa; }

/* 💡 사진 크기 확대 (45px -> 70px) 및 디자인 최적화 */
.avatar-placeholder {
  width: 70px;
  height: 70px;
  background-color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  color: #005a9f;
  margin: 0 auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border: 2px solid #fff;
  overflow: hidden; /* 💡 사진이 원 밖으로 나가지 않게 */
}

.profile-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name-info { font-weight: 800; color: #2c3e50; font-size: 14px; margin-top: 10px; line-height: 1.2; }
.user-email-text { font-size: 11px; color: #606266; margin-top: 4px; word-break: break-all; opacity: 0.8; }

.menu-list { flex: 1; overflow-y: auto; }
.group-title { display: flex; align-items: center; padding: 10px 15px; font-size: 13px; font-weight: 600; color: #303133; text-decoration: none; }
.sb-nav-link { padding: 8px 15px 8px 35px; font-size: 12px; color: #606266; text-decoration: none; display: flex; align-items: center; }
.sb-nav-link.is-active { background-color: #ecf5ff; color: #409eff; font-weight: bold; }
</style>
