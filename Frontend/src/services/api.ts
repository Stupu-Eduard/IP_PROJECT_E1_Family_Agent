import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type { GroupMemberDTO } from '../types/GroupMemberDTO'


export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token

  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      useAuthStore.getState().logout()

      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }

    return Promise.reject(error)
  },
)

export const familyApi = {
  getMembers: (familyId: number) =>
    api.get<GroupMemberDTO[]>(`/api/v1/families/${familyId}/members`),

  addMember: (familyId: number, email: string, role: string) =>
    api.post<GroupMemberDTO>(`/api/v1/families/${familyId}/members`, { email, role }),

  removeMember: (familyId: number, memberId: number) =>
    api.delete(`/api/v1/families/${familyId}/members/${memberId}`),
}

