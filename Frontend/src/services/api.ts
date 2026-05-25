import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type { GroupMemberDTO } from '../types/GroupMemberDTO'


export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
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

export interface InvitationDTO {
  id: number;
  familyId: number;
  familyName: string;
  invitedByName: string;
  role: string;
}

export const familyApi = {
  createFamily: (name: string) =>
    api.post<{ token: string; role: string; familyId: number }>('/api/v1/families', { name }),

  getMembers: (familyId: number) =>
    api.get<GroupMemberDTO[]>(`/api/v1/families/${familyId}/members`),

  inviteMember: (familyId: number, email: string, role: string) =>
    api.post<InvitationDTO>(`/api/v1/families/${familyId}/members`, { email, role }),

  updateMemberRole: (familyId: number, memberId: number, role: string) =>
    api.patch<GroupMemberDTO>(`/api/v1/families/${familyId}/members/${memberId}/role`, { role }),

  removeMember: (familyId: number, memberId: number) =>
    api.delete(`/api/v1/families/${familyId}/members/${memberId}`),

  deleteChildAccount: (familyId: number, memberId: number) =>
    api.delete(`/api/v1/families/${familyId}/members/${memberId}/account`),

  leaveFamily: (familyId: number) =>
    api.delete(`/api/v1/families/${familyId}/leave`),

  deleteFamily: (familyId: number) =>
    api.delete(`/api/v1/families/${familyId}`),
}

export const authApi = {
  refresh: () =>
    api.post<{ token: string; role: string }>('/api/v1/auth/refresh'),
}

export const invitationApi = {
  getPending: () =>
    api.get<InvitationDTO[]>('/api/v1/invitations/pending'),

  accept: (id: number) =>
    api.post<{ token: string; role: string }>(`/api/v1/invitations/${id}/accept`),

  decline: (id: number) =>
    api.post(`/api/v1/invitations/${id}/decline`),
}

export interface AvatarResponseDTO {
  avatarUrl: string
}

export const profileApi = {
  getAvatar: () =>
    api.get<AvatarResponseDTO>('/api/v1/profile/avatar'),

  updatePredefinedAvatar: (avatarId: string) =>
    api.patch<AvatarResponseDTO>('/api/v1/profile/avatar', { avatarId }),

  uploadAvatarFile: (formData: FormData) =>
    api.post<AvatarResponseDTO>('/api/v1/profile/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  deleteAccount: () =>
    api.delete('/api/v1/profile/account'),
}
