import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type { GroupMemberDTO } from '../types/GroupMemberDTO'


export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081',
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

export interface InvitationDTO {
  id: number;
  familyId: number;
  familyName: string;
  invitedByName: string;
  role: string;
}

//Family API

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

  leaveFamily: (familyId: number) =>
      api.delete(`/api/v1/families/${familyId}/leave`),

  deleteFamily: (familyId: number) =>
      api.delete(`/api/v1/families/${familyId}`),

  /** Șterge contul unui copil din familie (acțiune efectuată de un adult). */
  deleteChildAccount: (familyId: number, memberId: number) =>
      api.delete(`/api/v1/families/${familyId}/members/${memberId}/account`),

  /** Copilul solicită tranziția la statut adult. */
  requestAdultTransition: (familyId: number, memberId: number) =>
      api.post<{ message: string; status: string }>(`/api/v1/families/${familyId}/members/${memberId}/request-adult`),

  /** Owner-ul aprobă sau respinge o cerere de tranziție adult. */
  approveAdultTransition: (familyId: number, memberId: number, approve: boolean) =>
      api.post<{ message: string; approved: boolean; memberId: number; newToken?: string }>(
          `/api/v1/families/${familyId}/members/${memberId}/approve-adult`,
          { approve }
      ),

  /** Returnează lista cererilor de tranziție adult în așteptare. */
  getPendingAdultRequests: (familyId: number) =>
      api.get<GroupMemberDTO[]>(`/api/v1/families/${familyId}/adult-requests`),
}

//Auth API

export const authApi = {
  refresh: () =>
      api.post<{ token: string; role: string }>('/api/v1/auth/refresh'),
}

//Invitation API

export const invitationApi = {
  getPending: () =>
      api.get<InvitationDTO[]>('/api/v1/invitations/pending'),

  accept: (id: number) =>
      api.post<{ token: string; role: string }>(`/api/v1/invitations/${id}/accept`),

  decline: (id: number) =>
      api.post(`/api/v1/invitations/${id}/decline`),
}

//User / Profile API

export const userApi = {
  /** Returnează profilul utilizatorului curent. */
  getProfile: () =>
      api.get<{ id: number; name: string; email: string }>('/api/v1/users/me'),

  /**
   * Actualizează numele utilizatorului curent.
   * Returnează un nou token JWT cu claims actualizate.
   */
  updateProfile: (name: string) =>
      api.put<{ message: string; token: string; name: string }>('/api/v1/users/me', { name }),

  /**
   * Șterge contul propriu al utilizatorului curent (doar adulți).
   * Apelul duce la logout automat pe frontend.
   */
  deleteOwnAccount: () =>
      api.delete('/api/v1/users/me'),
}