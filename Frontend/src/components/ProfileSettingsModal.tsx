import React, { useState, useEffect, useRef } from 'react';
import { X, User, Trash2, Loader2, Check, AlertTriangle, Lock } from 'lucide-react';
import { useAuthStore } from '../store/authStore.ts';
import { userApi } from '../services/api.ts';
import { decodeJwtPayload } from '../utils/jwt.ts';

interface Props {
    open: boolean;
    onClose: () => void;
}

type Tab = 'profile' | 'danger';

export default function ProfileSettingsModal({ open, onClose }: Props) {
    const token = useAuthStore((s) => s.token);
    const setToken = useAuthStore((s) => s.setToken);
    const logout = useAuthStore((s) => s.logout);

    const payload = token ? (decodeJwtPayload(token) as any) : null;
    const isAdult =
        payload?.role === 'Parent' || payload?.role === 'Co-Parent';

    const [tab, setTab] = useState<Tab>('profile');

    /* ── Profil ─────────────────────────────────────────────────────────────── */
    const [name, setName] = useState('');
    const [initialName, setInitialName] = useState('');
    const [email, setEmail] = useState('');
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [saveSuccess, setSaveSuccess] = useState(false);

    /* ── Ștergere cont ──────────────────────────────────────────────────────── */
    const [deleteConfirm, setDeleteConfirm] = useState('');
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const CONFIRM_WORD = 'STERGE';

    const firstInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (!open) return;
        setTab('profile');
        setSaveError(null);
        setSaveSuccess(false);
        setDeleteConfirm('');
        setDeleteError(null);

        if (payload) {
            setName(payload.name ?? '');
            setInitialName(payload.name ?? '');
            setEmail(payload.sub ?? payload.email ?? '');
        }
        setTimeout(() => firstInputRef.current?.focus(), 100);
    }, [open]);

    const handleSaveProfile = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setSaveError('Numele nu poate fi gol.');
            return;
        }
        if (trimmed.length < 2) {
            setSaveError('Numele trebuie să aibă cel puțin 2 caractere.');
            return;
        }
        if (trimmed.length > 64) {
            setSaveError('Numele nu poate depăși 64 de caractere.');
            return;
        }
        setSaving(true);
        setSaveError(null);
        setSaveSuccess(false);
        try {
            const { data } = await userApi.updateProfile(trimmed);
            // Actualizăm token-ul global — toate componentele din aplicație vor vedea noul nume
            setToken(data.token);
            setName(data.name);
            setSaveSuccess(true);
            setTimeout(() => setSaveSuccess(false), 3000);
        } catch (err: any) {
            const msg =
                err?.response?.data?.message ??
                err?.response?.data ??
                'Eroare la salvarea profilului.';
            setSaveError(typeof msg === 'string' ? msg : 'Eroare la salvarea profilului.');
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (deleteConfirm.trim().toUpperCase() !== CONFIRM_WORD) return;
        setDeleting(true);
        setDeleteError(null);
        try {
            await userApi.deleteOwnAccount();
            logout();
            window.location.replace('/');
        } catch (err: any) {
            const msg =
                err?.response?.data?.message ??
                err?.response?.data ??
                'Eroare la ștergerea contului.';
            setDeleteError(typeof msg === 'string' ? msg : 'Eroare la ștergerea contului.');
        } finally {
            setDeleting(false);
        }
    };

    const isDirty = name.trim() !== initialName.trim();

    if (!open) return null;

    return (
        /* Backdrop */
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-[2px] p-4"
            onClick={(e) => e.target === e.currentTarget && onClose()}
        >
            <div
                className="bg-white rounded-[18px] w-full max-w-[480px] shadow-2xl overflow-hidden"
                style={{ animation: 'modal-in 0.18s cubic-bezier(.4,0,.2,1)' }}
            >
                {/* Header */}
                <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-[#EDE9E3]">
                    <h2 className="text-[17px] font-semibold text-[#2D2926]">Setări cont</h2>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-[8px] hover:bg-[#F5F1EC] text-[#9A8A7C] transition-colors"
                    >
                        <X size={18} />
                    </button>
                </div>

                {/* Tabs */}
                <div className="flex px-6 pt-4 gap-1 border-b border-[#EDE9E3]">
                    {[
                        { key: 'profile' as Tab, label: 'Profil', Icon: User },
                        ...(isAdult
                            ? [{ key: 'danger' as Tab, label: 'Ștergere cont', Icon: Trash2 }]
                            : []),
                    ].map(({ key, label, Icon }) => (
                        <button
                            key={key}
                            onClick={() => setTab(key)}
                            className={`flex items-center gap-1.5 px-3 py-2 text-[13px] font-medium rounded-t-[8px] border-b-2 transition-colors ${
                                tab === key
                                    ? 'border-[#C97B4B] text-[#C97B4B]'
                                    : 'border-transparent text-[#9A8A7C] hover:text-[#2D2926]'
                            }`}
                        >
                            <Icon size={14} />
                            {label}
                        </button>
                    ))}
                </div>

                {/* Content */}
                <div className="px-6 py-5">
                    {/* ── Tab: Profil ─────────────────────────────────────────────────── */}
                    {tab === 'profile' && (
                        <form onSubmit={handleSaveProfile} className="flex flex-col gap-4">
                            {/* Email (read-only) */}
                            <div>
                                <label className="block text-[12px] font-medium text-[#9A8A7C] mb-1.5">
                                    Email
                                </label>
                                <div className="flex items-center gap-2 bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] px-4 py-2.5">
                                    <Lock size={14} className="text-[#C4B9AC] shrink-0" />
                                    <span className="text-[13px] text-[#9A8A7C] select-all">{email}</span>
                                </div>
                                <p className="mt-1 text-[11px] text-[#C4B9AC]">
                                    Adresa de email nu poate fi modificată.
                                </p>
                            </div>

                            {/* Nume */}
                            <div>
                                <label
                                    htmlFor="ps-name"
                                    className="block text-[12px] font-medium text-[#2D2926] mb-1.5"
                                >
                                    Nume afișat
                                </label>
                                <input
                                    id="ps-name"
                                    ref={firstInputRef}
                                    type="text"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    maxLength={100}
                                    placeholder="Ex: Andrei Popescu"
                                    className="w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 px-4 text-[13px] focus:outline-none focus:border-[#C4B9AC] transition-colors"
                                    required
                                />
                            </div>

                            {/* Feedback */}
                            {saveError && (
                                <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-[10px] px-4 py-2.5 text-[13px] text-red-600">
                                    <AlertTriangle size={14} className="shrink-0" />
                                    {saveError}
                                </div>
                            )}
                            {saveSuccess && (
                                <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 rounded-[10px] px-4 py-2.5 text-[13px] text-emerald-700">
                                    <Check size={14} className="shrink-0" />
                                    Profilul a fost salvat cu succes!
                                </div>
                            )}

                            {/* Actions */}
                            <div className="flex justify-end gap-2 pt-1">
                                <button
                                    type="button"
                                    onClick={onClose}
                                    className="px-4 py-2 rounded-[10px] border border-[#EDE9E3] text-[13px] font-medium text-[#9A8A7C] hover:border-[#C4B9AC] transition-colors"
                                >
                                    Anulează
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving || !isDirty}
                                    className="px-5 py-2 rounded-[10px] bg-[#2D2926] text-white text-[13px] font-medium hover:opacity-90 disabled:opacity-50 transition-all flex items-center gap-2"
                                >
                                    {saving ? (
                                        <>
                                            <Loader2 size={14} className="animate-spin" />
                                            Se salvează...
                                        </>
                                    ) : saveSuccess ? (
                                        <>
                                            <Check size={14} />
                                            Salvat!
                                        </>
                                    ) : (
                                        'Salvează'
                                    )}
                                </button>
                            </div>
                        </form>
                    )}

                    {/* ── Tab: Ștergere cont ──────────────────────────────────────────── */}
                    {tab === 'danger' && isAdult && (
                        <div className="flex flex-col gap-4">
                            <div className="bg-red-50 border border-red-200 rounded-[12px] p-4">
                                <div className="flex items-start gap-3">
                                    <AlertTriangle size={18} className="text-red-500 shrink-0 mt-0.5" />
                                    <div>
                                        <p className="text-[13px] font-semibold text-red-700 mb-1">
                                            Acțiune ireversibilă
                                        </p>
                                        <p className="text-[12px] text-red-600 leading-relaxed">
                                            Ștergerea contului tău este permanentă. Toate datele tale vor fi
                                            eliminate din baza de date. Dacă ești singurul administrator al
                                            unei familii cu mai mulți membri, trebuie să transferi rolul sau
                                            să ștergi familia înainte de a-ți șterge contul.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <div>
                                <label
                                    htmlFor="ps-delete-confirm"
                                    className="block text-[12px] font-medium text-[#2D2926] mb-1.5"
                                >
                                    Scrie{' '}
                                    <span className="font-mono font-bold text-red-600">{CONFIRM_WORD}</span>{' '}
                                    pentru a confirma
                                </label>
                                <input
                                    id="ps-delete-confirm"
                                    type="text"
                                    value={deleteConfirm}
                                    onChange={(e) => setDeleteConfirm(e.target.value)}
                                    placeholder={CONFIRM_WORD}
                                    className="w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 px-4 text-[13px] focus:outline-none focus:border-red-400 font-mono transition-colors"
                                />
                            </div>

                            {deleteError && (
                                <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-[10px] px-4 py-2.5 text-[13px] text-red-600">
                                    <AlertTriangle size={14} className="shrink-0" />
                                    {deleteError}
                                </div>
                            )}

                            <button
                                onClick={handleDeleteAccount}
                                disabled={
                                    deleting || deleteConfirm.trim().toUpperCase() !== CONFIRM_WORD
                                }
                                className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-[10px] bg-red-600 text-white text-[13px] font-semibold hover:bg-red-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                            >
                                {deleting ? (
                                    <>
                                        <Loader2 size={14} className="animate-spin" />
                                        Se șterge contul...
                                    </>
                                ) : (
                                    <>
                                        <Trash2 size={14} />
                                        Șterge contul definitiv
                                    </>
                                )}
                            </button>
                        </div>
                    )}
                </div>
            </div>

            <style>{`
        @keyframes modal-in {
          from { opacity: 0; transform: scale(0.96) translateY(8px); }
          to   { opacity: 1; transform: scale(1)    translateY(0);   }
        }
      `}</style>
        </div>
    );
}