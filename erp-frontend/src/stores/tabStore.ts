import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export interface Tab {
  pgmId: string
  pgmNm: string
  path: string
}

export const useTabStore = defineStore('tab', () => {
  const router = useRouter()
  const tabs = ref<Tab[]>([])
  const activeTabId = ref<string>('')

  // 탭 추가
  const addTab = (tab: Tab) => {
    try {
      const exists = tabs.value.find(t => t.pgmId === tab.pgmId)
      if (!exists) {
        tabs.value.push(tab)
      }
      activeTabId.value = tab.pgmId
      router.push(tab.path).catch(err => {
        if (err.name !== 'NavigationDuplicated') console.error('탭 이동 실패:', err)
      })
    } catch (e) {
      console.error('탭 추가 중 오류:', e)
    }
  }

  // 탭 닫기
  const closeTab = (pgmId: string) => {
    const index = tabs.value.findIndex(t => t.pgmId === pgmId)
    if (index === -1) return

    const isRemovingActive = activeTabId.value === pgmId
    tabs.value.splice(index, 1)

    if (isRemovingActive) {
      if (tabs.value.length > 0) {
        const nextTab = tabs.value[index] || tabs.value[index - 1]
        activeTabId.value = nextTab.pgmId
        router.replace(nextTab.path)
      } else {
        activeTabId.value = ''
        router.replace('/')
      }
    }
  }

  // 탭 전체 닫기 (초기화용)
  const closeAllTabs = () => {
    tabs.value = []
    activeTabId.value = ''
    router.replace('/')
  }

  // 다른 탭 닫기
  const closeOtherTabs = (pgmId: string) => {
    const targetTab = tabs.value.find(t => t.pgmId === pgmId)
    if (targetTab) {
      tabs.value = [targetTab]
      activeTabId.value = pgmId
      router.push(targetTab.path)
    }
  }

  // 탭 선택
  const selectTab = (tab: Tab) => {
    activeTabId.value = tab.pgmId
    router.push(tab.path).catch(err => {
      if (err.name !== 'NavigationDuplicated') console.error('탭 선택 실패:', err)
    })
  }

  return { tabs, activeTabId, addTab, closeTab, selectTab, closeAllTabs, closeOtherTabs }
})
