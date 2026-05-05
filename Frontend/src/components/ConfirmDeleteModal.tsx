import { AlertTriangle, Trash2, X } from 'lucide-react'

interface ConfirmDeleteModalProps {
  open: boolean
  title?: string
  description?: string
  confirmLabel?: string
  cancelLabel?: string
  isLoading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export default function ConfirmDeleteModal({
  open,
  title = 'Ștergi cheltuiala?',
  description = 'Acțiunea nu poate fi anulată.',
  confirmLabel = 'Șterge',
  cancelLabel = 'Renunță',
  isLoading = false,
  onConfirm,
  onCancel,
}: ConfirmDeleteModalProps) {
  if (!open) return null

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-delete-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-md rounded-[18px] bg-white p-6 shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start gap-4">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-red-50 text-red-600">
            <AlertTriangle size={20} />
          </div>
          <div className="flex-1">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 id="confirm-delete-title" className="text-[18px] font-medium text-[#2D2926]">
                  {title}
                </h3>
                <p className="mt-2 text-[13px] leading-6 text-[#8C7E6E]">{description}</p>
              </div>
              <button
                type="button"
                onClick={onCancel}
                className="rounded-full p-1.5 text-[#8C7E6E] transition-colors hover:bg-[#FAF8F5] hover:text-[#2D2926]"
                aria-label="Închide dialogul"
              >
                <X size={16} />
              </button>
            </div>
          </div>
        </div>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-[10px] border border-[#EDE9E3] bg-white px-4 py-2.5 text-[13px] font-medium text-[#2D2926] transition-colors hover:border-[#C4B9AC]"
            disabled={isLoading}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="inline-flex items-center justify-center gap-2 rounded-[10px] bg-red-600 px-4 py-2.5 text-[13px] font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isLoading}
          >
            <Trash2 size={16} />
            {isLoading ? 'Se șterge...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

