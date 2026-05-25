export type AvatarVariant = 'F' | 'M'

export interface PredefinedAvatar {
    id: string
    bg: string
    fg: string
    variant: AvatarVariant
    label: string
}

export const PREDEFINED_AVATARS: PredefinedAvatar[] = [
    { id: 'av_f_pink',   bg: '#FBEAF0', fg: '#72243E', variant: 'F', label: 'Feminin Roz' },
    { id: 'av_m_pink',   bg: '#FBEAF0', fg: '#72243E', variant: 'M', label: 'Masculin Roz' },
    { id: 'av_f_purple', bg: '#EEEDFE', fg: '#3C3489', variant: 'F', label: 'Feminin Mov' },
    { id: 'av_m_purple', bg: '#EEEDFE', fg: '#3C3489', variant: 'M', label: 'Masculin Mov' },
    { id: 'av_f_blue',   bg: '#E6F1FB', fg: '#0C447C', variant: 'F', label: 'Feminin Albastru' },
    { id: 'av_m_blue',   bg: '#E6F1FB', fg: '#0C447C', variant: 'M', label: 'Masculin Albastru' },
    { id: 'av_f_dark',   bg: '#2C2C2A', fg: '#D3D1C7', variant: 'F', label: 'Feminin Negru' },
    { id: 'av_m_dark',   bg: '#2C2C2A', fg: '#D3D1C7', variant: 'M', label: 'Masculin Negru' },
    { id: 'av_f_yellow', bg: '#FAEEDA', fg: '#633806', variant: 'F', label: 'Feminin Galben' },
    { id: 'av_m_yellow', bg: '#FAEEDA', fg: '#633806', variant: 'M', label: 'Masculin Galben' },
]

export function findPredefinedAvatar(avatarUrl: string | null): PredefinedAvatar | null {
    if (!avatarUrl) return null
    return PREDEFINED_AVATARS.find((a) => a.id === avatarUrl) ?? null
}

export function isRemoteAvatarUrl(avatarUrl: string | null): boolean {
    if (!avatarUrl) return false
    return avatarUrl.startsWith('http') || avatarUrl.startsWith('blob')
}
