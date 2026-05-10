import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Camera, Save, Trash2, Bell, Languages, Palette } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { DEFAULT_USER_PREFERENCES } from '../types/UserProfileDTO'
import { updateCurrentUserProfile } from '../services/profile'
import { getProfileAvatarUrl, getProfileDisplayName, getProfileInitials } from '../utils/profile'

const MAX_AVATAR_SIZE = 2 * 1024 * 1024
const ACCEPTED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

export default function ProfileSettings() {
  const navigate = useNavigate()
  const token = useAuthStore((state) => state.token)
  const profile = useAuthStore((state) => state.profile)
  const updateProfile = useAuthStore((state) => state.updateProfile)

  const initialDisplayName = getProfileDisplayName(profile, token)
  const initialAvatarUrl = getProfileAvatarUrl(profile, token)

  const [name, setName] = useState(initialDisplayName)
  const [theme, setTheme] = useState(profile?.preferences.theme ?? DEFAULT_USER_PREFERENCES.theme)
  const [language, setLanguage] = useState(profile?.preferences.language ?? DEFAULT_USER_PREFERENCES.language)
  const [emailNotifications, setEmailNotifications] = useState(profile?.preferences.emailNotifications ?? DEFAULT_USER_PREFERENCES.emailNotifications)
  const [avatarFile, setAvatarFile] = useState<File | null>(null)
  const [avatarPreview, setAvatarPreview] = useState<string | null>(initialAvatarUrl)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const previewUrlRef = useRef<string | null>(null)

  useEffect(() => {
    setName(initialDisplayName)
    setTheme(profile?.preferences.theme ?? DEFAULT_USER_PREFERENCES.theme)
    setLanguage(profile?.preferences.language ?? DEFAULT_USER_PREFERENCES.language)
    setEmailNotifications(profile?.preferences.emailNotifications ?? DEFAULT_USER_PREFERENCES.emailNotifications)
    setAvatarFile(null)
    setAvatarPreview(initialAvatarUrl)
    setError('')
    setSuccess('')
  }, [initialAvatarUrl, initialDisplayName, profile])

  useEffect(() => {
    return () => {
      if (previewUrlRef.current) {
        URL.revokeObjectURL(previewUrlRef.current)
      }
    }
  }, [])

  const clearGeneratedPreview = () => {
    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current)
      previewUrlRef.current = null
    }
  }

  const handleAvatarChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) return

    if (!ACCEPTED_AVATAR_TYPES.includes(file.type)) {
      setError('Avatarul trebuie să fie JPG sau PNG.')
      setAvatarFile(null)
      return
    }

    if (file.size > MAX_AVATAR_SIZE) {
      setError('Avatarul nu poate depăși 2MB.')
      setAvatarFile(null)
      return
    }

    clearGeneratedPreview()
    const nextPreview = URL.createObjectURL(file)
    previewUrlRef.current = nextPreview
    setError('')
    setSuccess('')
    setAvatarFile(file)
    setAvatarPreview(nextPreview)
  }

  const handleRemoveAvatar = () => {
    clearGeneratedPreview()
    setAvatarFile(null)
    setAvatarPreview(null)
    setError('')
    setSuccess('')
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const trimmedName = name.trim()

    if (!trimmedName) {
      setError('Numele nu poate fi gol.')
      setSuccess('')
      return
    }

    setIsSaving(true)
    setError('')
    setSuccess('')

    try {
      const updatedProfile = await updateCurrentUserProfile({
        name: trimmedName,
        preferences: {
          theme,
          language,
          emailNotifications,
        },
        avatarFile,
      })

      updateProfile(updatedProfile)
      setAvatarFile(null)
      clearGeneratedPreview()
      setAvatarPreview(updatedProfile.avatarUrl)
      setSuccess('Modificările au fost salvate cu succes.')
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Nu am putut salva setările profilului.')
    } finally {
      setIsSaving(false)
    }
  }

  const initials = getProfileInitials(profile, token, name)

  return (
    <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[920px] mx-auto w-full flex-1">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate('/dashboard')} className="btn-alive-secondary !p-0 w-10 h-10 justify-center shrink-0">
          <ChevronLeft size={18} />
        </button>
        <div>
          <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">Setări profil</h2>
          <p className="text-[13px] text-[#9A8A7C]">Actualizează numele, avatarul și preferințele contului.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="bg-white border border-[#EDE9E3] rounded-[14px] p-6">
          <div className="flex items-center gap-2 mb-5 text-[14px] font-medium text-[#2D2926]">
            <Save size={18} className="text-[#C97B4B]" />
            Date profil
          </div>

          <label className="block mb-5">
            <span className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium uppercase">Nume</span>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-2 w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[14px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC]"
              placeholder="Numele tău"
            />
          </label>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium uppercase flex items-center gap-2"><Palette size={13} />Temă</span>
              <select value={theme} onChange={(e) => setTheme(e.target.value as typeof theme)} className="mt-2 w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[14px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC]">
                <option value="system">System</option>
                <option value="light">Light</option>
                <option value="dark">Dark</option>
              </select>
            </label>

            <label className="block">
              <span className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium uppercase flex items-center gap-2"><Languages size={13} />Limbă</span>
              <select value={language} onChange={(e) => setLanguage(e.target.value as typeof language)} className="mt-2 w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[14px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC]">
                <option value="ro">Română</option>
                <option value="en">English</option>
              </select>
            </label>
          </div>

          <label className="mt-5 flex items-center gap-3 cursor-pointer select-none">
            <input type="checkbox" checked={emailNotifications} onChange={(e) => setEmailNotifications(e.target.checked)} className="h-4 w-4 rounded border-[#D8CEC4] text-[#C97B4B] focus:ring-[#C97B4B]" />
            <span className="text-[14px] text-[#2D2926] flex items-center gap-2"><Bell size={14} className="text-[#8C7E6E]" />Primește notificări pe email</span>
          </label>

          {(error || success) && (
            <div className={`mt-5 rounded-[10px] px-4 py-3 text-[13px] ${error ? 'bg-[#FEF2F2] border border-[#FECACA] text-[#DC2626]' : 'bg-emerald-50 border border-emerald-100 text-emerald-700'}`}>
              {error ? `⚠ ${error}` : `✓ ${success}`}
            </div>
          )}
        </section>

        <section className="bg-white border border-[#EDE9E3] rounded-[14px] p-6">
          <div className="flex items-center gap-2 mb-5 text-[14px] font-medium text-[#2D2926]">
            <Camera size={18} className="text-[#C97B4B]" />
            Avatar
          </div>

          <div className="flex flex-col items-center text-center">
            <div className="w-32 h-32 rounded-[26px] overflow-hidden bg-[#FAF8F5] border border-[#EDE9E3] flex items-center justify-center text-[32px] font-medium text-[#2D2926] shadow-sm">
              {avatarPreview ? <img src={avatarPreview} alt="Preview avatar" className="w-full h-full object-cover" /> : initials}
            </div>

            <div className="mt-4 text-[13px] text-[#9A8A7C]">Formate acceptate: JPG/PNG · maxim 2MB</div>

            <label className="mt-5 inline-flex items-center gap-2 px-4 py-2.5 rounded-[10px] bg-[#2D2926] text-white text-[13px] font-medium cursor-pointer hover:opacity-90 transition-opacity">
              <UploadIcon />
              Încarcă avatar
              <input type="file" accept="image/png,image/jpeg" onChange={handleAvatarChange} className="hidden" />
            </label>

            <button type="button" onClick={handleRemoveAvatar} className="mt-3 inline-flex items-center gap-2 px-4 py-2.5 rounded-[10px] border border-[#EDE9E3] text-[13px] font-medium text-[#8C7E6E] hover:border-[#C4B9AC] hover:text-[#2D2926] transition-colors">
              <Trash2 size={14} />
              Elimină avatarul
            </button>
          </div>
        </section>

        <div className="lg:col-span-2 flex justify-end">
          <button type="submit" disabled={isSaving} className="bg-[#2D2926] text-white px-5 py-3 rounded-[10px] text-[14px] font-medium hover:opacity-90 disabled:opacity-60 transition-opacity inline-flex items-center gap-2">
            <Save size={16} />
            {isSaving ? 'Se salvează...' : 'Salvează modificările'}
          </button>
        </div>
      </form>
    </div>
  )
}

function UploadIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <polyline points="17 8 12 3 7 8" />
      <line x1="12" y1="3" x2="12" y2="15" />
    </svg>
  )
}

