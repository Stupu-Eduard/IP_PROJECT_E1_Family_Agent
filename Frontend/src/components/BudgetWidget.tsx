import { useState, useEffect } from 'react'
import { fetchBudget, saveBudget } from '../services/budget'

interface BudgetWidgetProps {
    currentMonthExpenses: number;
    userRole: string
}

// Praguri de culoare pentru bara de progres
const THRESHOLD_WARNING = 0.75
const THRESHOLD_DANGER  = 0.90

function getBarColor(ratio: number): string {
    if (ratio >= THRESHOLD_DANGER)  return '#E24B4A'  // roșu
    if (ratio >= THRESHOLD_WARNING) return '#F59E0B'  // galben
    return 'var(--color-primary)'                      // verde
}

function getStatusLabel(ratio: number): string {
    if (ratio >= THRESHOLD_DANGER)  return 'Depășit limita!'
    if (ratio >= THRESHOLD_WARNING) return 'Aproape de limită'
    return 'În limitele bugetului'
}

export default function BudgetWidget({ currentMonthExpenses, userRole, }: BudgetWidgetProps) {
    const isAdmin = userRole === 'Parent' || userRole === 'Co-Parent'

    const [monthlyLimit, setMonthlyLimit] = useState<number | null>(null)  //
    const [inputValue, setInputValue] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [saveSuccess, setSaveSuccess] = useState(false)
    const [validationError, setValidationError] = useState<string | null>(null)
    const [isEditing, setIsEditing] = useState(false)

    // ── Preluare buget curent la montare
    useEffect(() => {
        let cancelled = false
        const load = async () => {
            setIsLoading(true)
            try {
                const data = await fetchBudget()
                if (cancelled) return
                setMonthlyLimit(data.monthlyLimit)
                setInputValue(String(data.monthlyLimit))
            } catch {
                if (cancelled) return
                setMonthlyLimit(null)
            } finally {
                if (!cancelled) setIsLoading(false)
            }
        }
        void load()
        return () => { cancelled = true }
    }, [])

    // ── Validare și salvare
    const handleSave = async () => {
        setSaveError(null)
        setSaveSuccess(false)
        setValidationError(null)

        const parsed = parseFloat(inputValue)
        if (isNaN(parsed) || parsed <= 0) {
            setValidationError('Bugetul trebuie să fie un număr mai mare decât 0.')
            return
        }

        setIsSaving(true)
        try {
            const saved = await saveBudget(parsed)
            setMonthlyLimit(saved.monthlyLimit)
            setInputValue(String(saved.monthlyLimit))
            setSaveSuccess(true)
            setIsEditing(false)
            setTimeout(() => setSaveSuccess(false), 3000)
        } catch {
            setSaveError('Eroare la salvare. Încearcă din nou.')
        } finally {
            setIsSaving(false)
        }
    }

    const handleCancel = () => {
        setIsEditing(false)
        setValidationError(null)
        setSaveError(null)
        if (monthlyLimit !== null) setInputValue(String(monthlyLimit))
    }

    // ── Calcule pentru bara de progres
    const hasLimit  = monthlyLimit !== null && monthlyLimit > 0
    const ratio     = hasLimit ? Math.min(currentMonthExpenses / monthlyLimit!, 1) : 0
    const percent   = Math.round(ratio * 100)
    const barColor  = getBarColor(ratio)
    const statusLabel = getStatusLabel(ratio)

    return (
        <div className="card" data-testid="budget-widget">

            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div className="label">BUGET LUNAR</div>
                {isAdmin && !isEditing && !isLoading && (
                    <button
                        data-testid="edit-budget-btn"
                        className="btn btn-ghost"
                        style={{ padding: '6px 12px', fontSize: 12 }}
                        onClick={() => setIsEditing(true)}
                    >
                        {monthlyLimit ? 'Modifică' : 'Setează buget'}
                    </button>
                )}
            </div>

            {/* Loading */}
            {isLoading && (
                <div data-testid="budget-loading" style={{ color: 'var(--color-muted)', fontSize: 13 }}>
                    Se încarcă...
                </div>
            )}

            {/* Fără buget setat și fără editare */}
            {!isLoading && !hasLimit && !isEditing && (
                <div data-testid="no-budget-msg" style={{ color: 'var(--color-muted)', fontSize: 13 }}>
                    {isAdmin
                        ? 'Niciun buget setat. Apasă „Setează buget" pentru a defini o limită lunară.'
                        : 'Niciun buget definit pentru această lună.'}
                </div>
            )}

            {/* Bara de progres (când există buget) */}
            {!isLoading && hasLimit && !isEditing && (
                <div data-testid="budget-progress-section">
                    {/* Valori numerice */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
                        <div>
              <span style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.5px', color: 'var(--color-ink)' }}>
                {currentMonthExpenses.toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </span>
                            <span style={{ fontSize: 12, color: 'var(--color-muted-2)', marginLeft: 4 }}>RON</span>
                        </div>
                        <div style={{ fontSize: 13, color: 'var(--color-muted)' }}>
                            din{' '}
                            <strong style={{ color: 'var(--color-ink)' }}>
                                {monthlyLimit!.toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </strong>{' '}
                            RON
                        </div>
                    </div>

                    {/* Bara de progres */}
                    <div
                        data-testid="budget-progress-bar-track"
                        style={{
                            width: '100%', height: 10, borderRadius: 999,
                            background: 'var(--color-border)', overflow: 'hidden', marginBottom: 8,
                        }}
                    >
                        <div
                            data-testid="budget-progress-bar-fill"
                            style={{
                                height: '100%',
                                width: `${percent}%`,
                                borderRadius: 999,
                                background: barColor,
                                transition: 'width 0.5s ease, background 0.4s ease',
                            }}
                        />
                    </div>

                    {/* Status și procent */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span
                data-testid="budget-status-label"
                style={{ fontSize: 12, color: barColor, fontWeight: 500 }}
            >
              {statusLabel}
            </span>
                        <span
                            data-testid="budget-percent"
                            style={{ fontSize: 12, color: 'var(--color-muted)', fontWeight: 500 }}
                        >
              {percent}%
            </span>
                    </div>

                    {/* Avertizare vizuală la pragul de 90% */}
                    {ratio >= THRESHOLD_DANGER && (
                        <div
                            data-testid="budget-danger-alert"
                            style={{
                                marginTop: 12, padding: '10px 14px', borderRadius: 10,
                                background: '#FEF2F2', border: '1px solid #FECACA',
                                fontSize: 12, color: '#B91C1C', display: 'flex', alignItems: 'center', gap: 8,
                            }}
                        >
                            ⚠ Cheltuielile lunii curente au depășit {Math.round(THRESHOLD_DANGER * 100)}% din bugetul setat!
                        </div>
                    )}

                    {/* Avertizare vizuală la pragul de 75% */}
                    {ratio >= THRESHOLD_WARNING && ratio < THRESHOLD_DANGER && (
                        <div
                            data-testid="budget-warning-alert"
                            style={{
                                marginTop: 12, padding: '10px 14px', borderRadius: 10,
                                background: '#FFFBEB', border: '1px solid #FDE68A',
                                fontSize: 12, color: '#92400E', display: 'flex', alignItems: 'center', gap: 8,
                            }}
                        >
                            ⚡ Te apropii de limita bugetului lunar. Mai ai {100 - percent}% disponibil.
                        </div>
                    )}
                </div>
            )}

            {/* ── Formular editare buget (doar Admin) ─────────────────────────── */}
            {!isLoading && isEditing && isAdmin && (
                <div data-testid="budget-edit-form">
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--color-muted)', marginBottom: 6 }}>
                        Limită lunară (RON)
                    </label>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                        <input
                            data-testid="budget-input"
                            type="number"
                            min="1"
                            step="any"
                            value={inputValue}
                            onChange={(e) => {
                                setInputValue(e.target.value)
                                setValidationError(null)
                                setSaveError(null)
                            }}
                            placeholder="ex: 3000"
                            style={{
                                flex: 1, padding: '10px 14px', borderRadius: 12, fontSize: 14,
                                border: `1px solid ${validationError ? '#E24B4A' : 'var(--color-border)'}`,
                                outline: 'none', fontFamily: 'inherit', color: 'var(--color-ink)',
                                background: '#fff',
                            }}
                        />
                        <button
                            data-testid="budget-save-btn"
                            className="btn btn-primary"
                            style={{ padding: '10px 18px', fontSize: 13 }}
                            onClick={handleSave}
                            disabled={isSaving}
                        >
                            {isSaving ? 'Se salvează...' : 'Salvează'}
                        </button>
                        <button
                            data-testid="budget-cancel-btn"
                            className="btn btn-ghost"
                            style={{ padding: '10px 14px', fontSize: 13 }}
                            onClick={handleCancel}
                            disabled={isSaving}
                        >
                            Anulează
                        </button>
                    </div>

                    {/* Eroare validare */}
                    {validationError && (
                        <div
                            data-testid="budget-validation-error"
                            style={{ marginTop: 8, fontSize: 12, color: '#E24B4A' }}
                        >
                            {validationError}
                        </div>
                    )}

                    {/* Eroare API */}
                    {saveError && (
                        <div
                            data-testid="budget-save-error"
                            style={{ marginTop: 8, fontSize: 12, color: '#E24B4A' }}
                        >
                            {saveError}
                        </div>
                    )}
                </div>
            )}

            {/* Confirmare succes salvare */}
            {saveSuccess && (
                <div
                    data-testid="budget-success-msg"
                    style={{
                        marginTop: 12, padding: '8px 14px', borderRadius: 10,
                        background: '#F0FDF4', border: '1px solid #BBF7D0',
                        fontSize: 12, color: '#15803D',
                    }}
                >
                    ✓ Bugetul a fost salvat cu succes!
                </div>
            )}
        </div>
    )
}