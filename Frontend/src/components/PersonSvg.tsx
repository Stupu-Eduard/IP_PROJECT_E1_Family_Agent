import type { AvatarVariant } from '../utils/avatars'

interface PersonSvgProps {
    fg: string
    variant: AvatarVariant
    size?: number
}

export default function PersonSvg({ fg, variant, size = 26 }: PersonSvgProps) {
    if (variant === 'F') {
        return (
            <svg
                viewBox="0 0 40 40"
                xmlns="http://www.w3.org/2000/svg"
                width={size}
                height={size}
                aria-hidden="true"
            >
                <circle cx="20" cy="14" r="7" fill={fg} />
                <path d="M10 40 C10 30 13 27 20 27 C27 27 30 30 30 40 Z" fill={fg} />
            </svg>
        )
    }

    return (
        <svg
            viewBox="0 0 40 40"
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            aria-hidden="true"
        >
            <circle cx="20" cy="14" r="7.5" fill={fg} />
            <path d="M5 40 C5 29 9 27 20 27 C31 27 35 29 35 40 Z" fill={fg} />
        </svg>
    )
}
