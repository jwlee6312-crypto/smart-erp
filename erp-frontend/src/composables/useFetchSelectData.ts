import { api } from '@/utils/axios'
import type { AxiosResponse } from 'axios'

export interface SelectData {
	CODECD: string
	CODENM: string
}

/**
 * 🅰️ ERP (MSSQL) 공통코드 조회
 */
export async function fetchSelectData(CDTYPE: string): Promise<SelectData[]> {
	return fetchSelectDataList<SelectData>('/code', { CDTYPE })
}

/**
 * 🅱️ CRM (MSSQL 통합) 공통코드 조회
 */
export async function fetchCrmSelectData(CDTYPE: string): Promise<SelectData[]> {
	return fetchSelectDataList<SelectData>('/crm/outbound/code', { CDTYPE })
}

/**
 * 범용 셀렉트 데이터 조회 (대문자 표준)
 */
export async function fetchSelectDataList<T>(url: string, params?: any): Promise<T[]> {
	try {
		let requestParams: any = {};

		if (typeof params === 'string') {
			requestParams = { CDTYPE: params };
		} else if (params) {
			Object.keys(params).forEach(key => {
				requestParams[key.toUpperCase()] = params[key];
			});
		}

		const res: AxiosResponse<any> = await api.get(url, {
			params: requestParams,
		})

		const list = res.data || []
		return Array.isArray(list) ? list : []
	} catch (e) {
		console.error(`[fetchSelectDataList] ${url} 조회 실패:`, e)
		return []
	}
}

/**
 * 유저 정보 조회
 */
export async function fetchUserData(): Promise<any[]> {
	return fetchSelectDataList<any>('/user', {})
}

/**
 * 🚀 생산라인 정보 조회 (HP00_000S_STR 연동)
 */
export interface SelectPdLineData {
	linecd: string;
	linenm: string;
}

export async function fetchLineData(): Promise<SelectPdLineData[]> {
	return fetchSelectDataList<SelectPdLineData>('/hp00/HP00_000S_STR', { GUBUN: 'L0' })
}

/**
 * 🚀 생산공정 정보 조회 (HP00_000S_STR 연동)
 */
export interface SelectPdProgData {
	progcd: string;
	prognm: string;
}

export async function fetchProgData(linecd: string): Promise<SelectPdProgData[]> {
	return fetchSelectDataList<SelectPdProgData>('/hp00/HP00_000S_STR', { GUBUN: 'G0', GBNCD: linecd })
}
