import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { PREDEFINED_AVATARS, findPredefinedAvatar, isRemoteAvatarUrl } from '../utils/avatars'
import PersonSvg from '../components/PersonSvg'
import { profileApi } from '../services/api'
import { decodeJwtPayload } from '../utils/jwt'

const MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const DELETE_CONFIRMATION_PHRASE = 'STERGE CONT'

// ── Modal confirmare ștergere cont ────────────────────────────────────────────
interface DeleteAccountModalProps {
    onClose: () => void
    onConfirm: () => void
    loading: boolean
}

function DeleteAccountModal({ onClose, onConfirm, loading }: DeleteAccountModalProps) {
    const [inputValue, setInputValue] = useState('')
    const canConfirm = inputValue === DELETE_CONFIRMATION_PHRASE

    return (
        <div
            className="fa-modal-backdrop"
            onClick={onClose}
            onKeyDown={(e) => e.key === 'Escape' && onClose()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-modal-title"
        >
            <div
                className="fa-modal"
                onClick={(e) => e.stopPropagation()}
            >
                <h3 id="delete-modal-title" className="fa-modal-title">
                    Confirmare ștergere cont
                </h3>
                <p className="fa-modal-body">
                    Această acțiune este ireversibilă. Toate datele tale vor fi șterse permanent.
                    Tastează <strong>{DELETE_CONFIRMATION_PHRASE}</strong> pentru a confirma.
                </p>
                <input
                    className="fa-input"
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder={DELETE_CONFIRMATION_PHRASE}
                    autoFocus
                />
                <div className="fa-modal-actions">
                    <button className="fa-btn fa-btn-secondary" onClick={onClose} disabled={loading}>
                        Anulează
                    </button>
                    <button
                        className="fa-btn fa-btn-danger"
                        onClick={onConfirm}
                        disabled={!canConfirm || loading}
                    >
                        {loading ? 'Se șterge…' : 'Șterge contul'}
                    </button>
                </div>
            </div>
        </div>
    )
}

// ── Secțiune avatar ───────────────────────────────────────────────────────────
interface AvatarSelectionSectionProps {
    currentAvatarUrl: string | null
    onSaved: (url: string) => void
}

function AvatarSelectionSection({ currentAvatarUrl, onSaved }: AvatarSelectionSectionProps) {
    const [selected, setSelected] = useState<string | null>(currentAvatarUrl)
    const [preview, setPreview] = useState<string | null>(
        isRemoteAvatarUrl(currentAvatarUrl) ? currentAvatarUrl : null
    )
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const prevObjectUrl = useRef<string | null>(null)
    const fileInputRef = useRef<HTMLInputElement>(null)
    const pendingFile = useRef<File | null>(null)

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        if (!ALLOWED_MIME_TYPES.includes(file.type)) {
            setError('Format nepermis. Folosește JPG, PNG, WEBP sau GIF.')
            return
        }
        if (file.size > MAX_FILE_SIZE_BYTES) {
            setError('Imaginea depășește 2 MB.')
            return
        }

        setError(null)
        if (prevObjectUrl.current) {
            URL.revokeObjectURL(prevObjectUrl.current)
        }
        const objectUrl = URL.createObjectURL(file)
        prevObjectUrl.current = objectUrl
        pendingFile.current = file
        setPreview(objectUrl)
        setSelected(objectUrl)
    }

    const handleSave = async () => {
        if (!selected) return
        setSaving(true)
        setError(null)
        try {
            if (pendingFile.current) {
                const formData = new FormData()
                formData.append('file', pendingFile.current, pendingFile.current.name)
                const res = await profileApi.uploadAvatarFile(formData)
                pendingFile.current = null
                onSaved(res.data.avatarUrl)
            } else if (isRemoteAvatarUrl(selected) && !selected.startsWith('blob')) {
                onSaved(selected)
            } else {
                await profileApi.updatePredefinedAvatar(selected)
                onSaved(selected)
            }
        } catch {
            setError('Eroare la salvare. Încearcă din nou.')
        } finally {
            setSaving(false)
        }
    }

    const currentPredefined = findPredefinedAvatar(selected)

    return (
        <section className="fa-card" style={{ marginBottom: '1.5rem' }}>
            <h2 className="fa-section-title">Avatar</h2>

            {/* Preview curent */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
                <div
                    style={{
                        width: 64,
                        height: 64,
                        borderRadius: '50%',
                        overflow: 'hidden',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background: currentPredefined ? currentPredefined.bg : '#EDE9E3',
                        border: '2px solid #C97B4B',
                        flexShrink: 0,
                    }}
                >
                    {preview ? (
                        <img src={preview} alt="Avatar preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : currentPredefined ? (
                        <PersonSvg fg={currentPredefined.fg} variant={currentPredefined.variant} size={38} />
                    ) : (
                        <span style={{ fontSize: 28, color: '#9A8A7C' }}>?</span>
                    )}
                </div>
                <div>
                    <div style={{ fontWeight: 600, color: '#2D2926' }}>
                        {currentPredefined ? currentPredefined.label : preview ? 'Poză personalizată' : 'Niciun avatar selectat'}
                    </div>
                    <div style={{ fontSize: 13, color: '#9A8A7C' }}>Selectează din opțiunile de mai jos</div>
                </div>
            </div>

            {/* Grid avataruri predefinite */}
            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(5, 1fr)',
                    gap: '0.5rem',
                    marginBottom: '1.25rem',
                }}
            >
                {PREDEFINED_AVATARS.map((av) => {
                    const isActive = selected === av.id
                    return (
                        <button
                            key={av.id}
                            title={av.label}
                            onClick={() => {
                                setSelected(av.id)
                                setPreview(null)
                            }}
                            style={{
                                width: '100%',
                                aspectRatio: '1',
                                borderRadius: '50%',
                                background: av.bg,
                                border: isActive ? '2.5px solid #C97B4B' : '2px solid transparent',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                transition: 'border-color 0.15s',
                                outline: isActive ? '2px solid #C97B4B40' : 'none',
                                outlineOffset: 2,
                            }}
                        >
                            <PersonSvg fg={av.fg} variant={av.variant} size={28} />
                        </button>
                    )
                })}
            </div>

            {/* Upload poză */}
            <div style={{ marginBottom: '1rem' }}>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept={ALLOWED_MIME_TYPES.join(',')}
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                />
                <button
                    className="fa-btn fa-btn-secondary"
                    style={{ width: '100%' }}
                    onClick={() => fileInputRef.current?.click()}
                >
                    Încarcă o poză
                </button>
            </div>

            {error && (
                <div style={{ color: '#c0392b', fontSize: 13, marginBottom: '0.75rem' }}>{error}</div>
            )}

            <button
                className="fa-btn fa-btn-primary"
                style={{ width: '100%' }}
                onClick={handleSave}
                disabled={saving || !selected}
            >
                {saving ? 'Se salvează…' : 'Salvează avatarul'}
            </button>
        </section>
    )
}

// ── Secțiune ștergere cont ────────────────────────────────────────────────────
function DeleteAccountSection() {
    const [showModal, setShowModal] = useState(false)
    const [deleting, setDeleting] = useState(false)
    const logout = useAuthStore((s) => s.logout)
    const navigate = useNavigate()

    const handleConfirm = async () => {
        setDeleting(true)
        try {
            await profileApi.deleteAccount()
            logout()
            navigate('/login', { replace: true })
        } catch {
            setDeleting(false)
            setShowModal(false)
        }
    }

    return (
        <>
            <section className="fa-card">
                <h2 className="fa-section-title" style={{ color: '#c0392b' }}>Ștergere cont</h2>
                <p style={{ color: '#9A8A7C', fontSize: 14, marginBottom: '1rem' }}>
                    Odată șters, contul și toate datele asociate sunt eliminate permanent. Această acțiune nu poate fi anulată.
                </p>
                <button
                    className="fa-btn fa-btn-danger"
                    onClick={() => setShowModal(true)}
                >
                    Șterge contul meu
                </button>
            </section>

            {showModal && (
                <DeleteAccountModal
                    onClose={() => setShowModal(false)}
                    onConfirm={handleConfirm}
                    loading={deleting}
                />
            )}
        </>
    )
}

// ── Pagina principală ─────────────────────────────────────────────────────────
export default function ProfileSettings() {
    const token = useAuthStore((s) => s.token)
    const avatarUrl = useAuthStore((s) => s.avatarUrl)
    const setAvatar = useAuthStore((s) => s.setAvatar)

    let userRole = 'Parent'
    let userName = 'Utilizator'

    if (token) {
        const payload = decodeJwtPayload(token) as { role?: string; name?: string; sub?: string } | null
        userRole = payload?.role || 'Parent'
        const fullName = payload?.name || payload?.sub || ''
        if (fullName) {
            const parts = fullName.split(' ')
            userName = parts[0] || fullName
        }
    }

    const isChild = userRole === 'Child'

    return (
        <div style={{ maxWidth: 600, margin: '0 auto', padding: '2rem 1rem' }}>
            <h1 style={{ fontSize: 22, fontWeight: 700, color: '#2D2926', marginBottom: '0.25rem' }}>
                Setări profil
            </h1>
            <p style={{ color: '#9A8A7C', fontSize: 14, marginBottom: '2rem' }}>
                Bună, {userName}! Personalizează-ți contul.
            </p>

            <AvatarSelectionSection
                currentAvatarUrl={avatarUrl}
                onSaved={(url) => setAvatar(url)}
            />

            {!isChild && <DeleteAccountSection />}
        </div>
    )
}
