import { decodeJwtPayload } from '../utils/jwt';
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import type { GroupMemberDTO } from '../types/GroupMemberDTO';
import { familyApi, invitationApi, authApi, type InvitationDTO, api } from '../services/api';
import {
    Mail, UserPlus, Trash2, Shield,
    ArrowLeft, Baby, Crown, Loader2, Check, LogOut, Bell, FolderX
} from 'lucide-react';

interface ChildBudgetState {
    amount: number;
    input: string;
    saving: boolean;
    error: string | null;
    success: boolean;
}

export default function FamilySettings() {
    const token = useAuthStore((state) => state.token);
    const navigate = useNavigate();

    const payload = token ? decodeJwtPayload(token) : null;
    const currentUserRole = (payload as any)?.role ?? 'Child';
    const currentUserId: number = (payload as any)?.userId;
    const familyId: number | null = (payload as any)?.familyId ?? null;
    const isAdult = currentUserRole === 'Parent' || currentUserRole === 'Co-Parent';

    const [members, setMembers] = useState<GroupMemberDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [inviteEmail, setInviteEmail] = useState('');
    const [inviteRole, setInviteRole] = useState<'Co-Parent' | 'Child'>('Child');
    const [isAdding, setIsAdding] = useState(false);
    const [addError, setAddError] = useState<string | null>(null);
    const [addSuccess, setAddSuccess] = useState(false);

    const [pendingInvitations, setPendingInvitations] = useState<InvitationDTO[]>([]);
    const [invActionId, setInvActionId] = useState<number | null>(null);

    const [isLeaving, setIsLeaving] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    const [familyName, setFamilyName] = useState('');
    const [isCreating, setIsCreating] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);

    const [childBudgets, setChildBudgets] = useState<Record<number, ChildBudgetState>>({});
    const [roleChanging, setRoleChanging] = useState<number | null>(null);

    const loadMembers = useCallback(async () => {
        if (!familyId) return;
        try {
            setError(null);
            const { data } = await familyApi.getMembers(familyId);
            setMembers(data);
        } catch (err: any) {
            if (err?.response?.status === 403) {
                try {
                    const { data } = await authApi.refresh();
                    useAuthStore.getState().setToken(data.token);
                    window.location.reload();
                } catch {
                    useAuthStore.getState().logout();
                    window.location.replace('/login');
                }
            } else {
                setError('Nu s-au putut încărca membrii familiei.');
            }
        } finally {
            setLoading(false);
        }
    }, [familyId]);

    useEffect(() => {
        loadMembers();
        if (!familyId) return;
        const id = setInterval(loadMembers, 20000);
        return () => clearInterval(id);
    }, [loadMembers, familyId]);

    useEffect(() => {
        const load = () => invitationApi.getPending().then(r => setPendingInvitations(r.data)).catch(() => {});
        load();
        if (familyId) return;
        const id = setInterval(load, 15000);
        return () => clearInterval(id);
    }, [familyId]);

    // Încarcă bugetele copiilor după ce s-au încărcat membrii
    useEffect(() => {
        if (!isAdult || members.length === 0) return;
        const children = members.filter(m => m.role === 'Child');
        children.forEach(child => {
            api.get<number>(`/api/v1/budgets/child/${child.userId}`)
                .then(r => {
                    const amount = Number(r.data) || 0;
                    setChildBudgets(prev => ({
                        ...prev,
                        [child.userId]: { amount, input: amount > 0 ? String(amount) : '', saving: false, error: null, success: false },
                    }));
                })
                .catch(() => {
                    setChildBudgets(prev => ({
                        ...prev,
                        [child.userId]: { amount: 0, input: '', saving: false, error: null, success: false },
                    }));
                });
        });
    }, [isAdult, members]);

    const handleInviteMember = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!inviteEmail.trim() || !familyId) return;

        setIsAdding(true);
        setAddError(null);
        setAddSuccess(false);
        try {
            await familyApi.inviteMember(familyId, inviteEmail.trim(), inviteRole);
            setInviteEmail('');
            setAddSuccess(true);
            setTimeout(() => setAddSuccess(false), 3000);
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Eroare la trimiterea invitației.';
            setAddError(typeof msg === 'string' ? msg : 'Eroare la trimiterea invitației.');
        } finally {
            setIsAdding(false);
        }
    };

    const handleCreateFamily = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsCreating(true);
        setCreateError(null);
        try {
            const { data } = await familyApi.createFamily(familyName.trim());
            useAuthStore.getState().setToken(data.token);
            window.location.reload();
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Eroare la crearea familiei.';
            setCreateError(typeof msg === 'string' ? msg : 'Eroare la crearea familiei.');
        } finally {
            setIsCreating(false);
        }
    };

    const handleAcceptInvitation = async (inv: InvitationDTO) => {
        setInvActionId(inv.id);
        try {
            const { data } = await invitationApi.accept(inv.id);
            useAuthStore.getState().setToken(data.token);
            setPendingInvitations([]);
            window.location.reload();
        } catch (err: any) {
            alert(err?.response?.data?.message ?? 'Eroare la acceptarea invitației.');
        } finally {
            setInvActionId(null);
        }
    };

    const handleDeclineInvitation = async (inv: InvitationDTO) => {
        setInvActionId(inv.id);
        try {
            await invitationApi.decline(inv.id);
            setPendingInvitations(prev => prev.filter(i => i.id !== inv.id));
        } catch (err: any) {
            alert(err?.response?.data?.message ?? 'Eroare la refuzarea invitației.');
        } finally {
            setInvActionId(null);
        }
    };

    const handleLeaveFamily = async () => {
        if (!familyId) return;
        if (!window.confirm('Ești sigur că vrei să ieși din familie?')) return;
        setIsLeaving(true);
        try {
            await familyApi.leaveFamily(familyId);
            const { data } = await authApi.refresh();
            useAuthStore.getState().setToken(data.token);
            window.location.reload();
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? 'Eroare la ieșirea din familie.';
            alert(typeof msg === 'string' ? msg : 'Eroare la ieșirea din familie.');
        } finally {
            setIsLeaving(false);
        }
    };

    const handleDeleteFamily = async () => {
        if (!familyId) return;
        if (!window.confirm('Ești sigur că vrei să ștergi familia? Această acțiune este ireversibilă.')) return;
        setIsDeleting(true);
        try {
            await familyApi.deleteFamily(familyId);
            const { data } = await authApi.refresh();
            useAuthStore.getState().setToken(data.token);
            window.location.reload();
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? 'Eroare la ștergerea familiei.';
            alert(typeof msg === 'string' ? msg : 'Eroare la ștergerea familiei.');
        } finally {
            setIsDeleting(false);
        }
    };

    const handleRoleChange = async (member: GroupMemberDTO, newRole: string) => {
        if (!familyId) return;
        setRoleChanging(member.id);
        try {
            const { data } = await familyApi.updateMemberRole(familyId, member.id, newRole);
            setMembers(prev => prev.map(m => m.id === member.id ? { ...m, role: data.role } : m));
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? 'Eroare la schimbarea rolului.';
            alert(typeof msg === 'string' ? msg : 'Eroare la schimbarea rolului.');
        } finally {
            setRoleChanging(null);
        }
    };

    const handleRemoveMember = async (member: GroupMemberDTO) => {
        if (!familyId) return;
        const label = member.name || member.email;
        if (!window.confirm(`Ești sigur că dorești să elimini membrul ${label}?`)) return;
        try {
            await familyApi.removeMember(familyId, member.id);
            setMembers(prev => prev.filter(m => m.id !== member.id));
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Eroare la ștergerea membrului.';
            alert(typeof msg === 'string' ? msg : 'Eroare la ștergerea membrului.');
        }
    };

    const handleSetChildBudget = async (e: React.FormEvent, childUserId: number) => {
        e.preventDefault();
        const state = childBudgets[childUserId];
        const amount = parseFloat(state?.input ?? '');
        if (isNaN(amount) || amount < 0) {
            setChildBudgets(prev => ({ ...prev, [childUserId]: { ...prev[childUserId], error: 'Sumă invalidă.' } }));
            return;
        }
        setChildBudgets(prev => ({ ...prev, [childUserId]: { ...prev[childUserId], saving: true, error: null, success: false } }));
        try {
            const { data } = await api.put<number>(`/api/v1/budgets/child/${childUserId}`, { amount });
            setChildBudgets(prev => ({
                ...prev,
                [childUserId]: { amount: Number(data), input: String(data), saving: false, error: null, success: true },
            }));
            setTimeout(() => setChildBudgets(prev => ({ ...prev, [childUserId]: { ...prev[childUserId], success: false } })), 3000);
        } catch (err: any) {
            const msg = err?.response?.data?.message ?? 'Eroare la salvare.';
            setChildBudgets(prev => ({
                ...prev,
                [childUserId]: { ...prev[childUserId], saving: false, error: typeof msg === 'string' ? msg : 'Eroare la salvare.' },
            }));
        }
    };

    const getRoleIcon = (role: string) => {
        if (role === 'Parent') return <Crown size={18} className="text-[#C97B4B]" />;
        if (role === 'Co-Parent') return <Shield size={18} className="text-[#B5956A]" />;
        return <Baby size={18} className="text-[#8C7E6E]" />;
    };

    if (!familyId) {
        return (
            <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[800px] mx-auto w-full flex-1">
                <div className="flex items-center gap-4 mb-8">
                    <button onClick={() => navigate('/dashboard')} className="btn-alive-secondary !p-0 w-10 h-10 justify-center shrink-0">
                        <ArrowLeft size={18} />
                    </button>
                    <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">Familie</h2>
                </div>

                <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 mb-6">
                    <h3 className="text-[14px] font-medium text-[#2D2926] mb-4 flex items-center gap-2">
                        <UserPlus size={18} className="text-[#C97B4B]" />
                        Creează o familie nouă
                    </h3>
                    <form onSubmit={handleCreateFamily} className="flex gap-3">
                        <input
                            type="text"
                            value={familyName}
                            onChange={(e) => setFamilyName(e.target.value)}
                            placeholder="Numele familiei (ex: Familia Popescu)"
                            className="flex-1 bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 px-4 text-[13px] focus:outline-none focus:border-[#C4B9AC]"
                            required
                        />
                        <button
                            type="submit"
                            disabled={isCreating}
                            className="bg-[#2D2926] text-white px-5 py-2.5 rounded-[10px] text-[13px] font-medium hover:opacity-90 disabled:opacity-50 transition-all flex items-center gap-2 shrink-0"
                        >
                            {isCreating ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                            {isCreating ? 'Se creează...' : 'Creează'}
                        </button>
                    </form>
                    {createError && <p className="mt-3 text-[12px] text-red-500">{createError}</p>}
                </div>

                {pendingInvitations.length > 0 && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden">
                        <div className="px-6 py-4 border-b border-[#EDE9E3] flex items-center gap-2">
                            <Bell size={16} className="text-[#C97B4B]" />
                            <span className="text-[14px] font-medium text-[#2D2926]">Invitații în așteptare</span>
                        </div>
                        {pendingInvitations.map(inv => (
                            <div key={inv.id} className="px-6 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#EDE9E3] last:border-0">
                                <div>
                                    <div className="text-[15px] font-medium text-[#2D2926]">{inv.familyName}</div>
                                    <div className="text-[12px] text-[#9A8A7C] mt-0.5">
                                        Invitat de <span className="font-medium">{inv.invitedByName}</span> cu rolul <span className="font-medium">{inv.role}</span>
                                    </div>
                                </div>
                                <div className="flex gap-2 shrink-0">
                                    <button
                                        disabled={invActionId === inv.id}
                                        onClick={() => handleAcceptInvitation(inv)}
                                        className="bg-[#2D2926] text-white px-4 py-2 rounded-[10px] text-[13px] font-medium hover:opacity-90 disabled:opacity-50 flex items-center gap-1.5"
                                    >
                                        {invActionId === inv.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
                                        Acceptă
                                    </button>
                                    <button
                                        disabled={invActionId === inv.id}
                                        onClick={() => handleDeclineInvitation(inv)}
                                        className="bg-white border border-[#EDE9E3] text-[#2D2926] px-4 py-2 rounded-[10px] text-[13px] font-medium hover:border-[#C4B9AC] disabled:opacity-50"
                                    >
                                        Refuză
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    return (
        <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[800px] mx-auto w-full flex-1">
            <div className="flex items-center gap-4 mb-8 stagger-1">
                <button onClick={() => navigate('/dashboard')} className="btn-alive-secondary !p-0 w-10 h-10 justify-center shrink-0">
                    <ArrowLeft size={18} />
                </button>
                <div className="flex-1">
                    <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">
                        {isAdult ? 'Gestionare Familie' : 'Membrii Familiei Mele'}
                    </h2>
                    <p className="text-[13px] text-[#9A8A7C]">
                        {isAdult ? 'Administrează rolurile, accesul și bugetele membrilor' : 'Vezi cine mai face parte din grupul tău'}
                    </p>
                </div>
                <div className="flex gap-2 shrink-0">
                    {isAdult && members.length === 1 && (
                        <button
                            onClick={handleDeleteFamily}
                            disabled={isDeleting}
                            className="flex items-center gap-2 px-4 py-2 rounded-[10px] border border-red-300 bg-red-50 text-red-600 text-[13px] font-medium hover:bg-red-100 disabled:opacity-50 transition-all"
                        >
                            {isDeleting ? <Loader2 size={15} className="animate-spin" /> : <FolderX size={15} />}
                            Șterge familia
                        </button>
                    )}
                    <button
                        onClick={handleLeaveFamily}
                        disabled={isLeaving}
                        className="flex items-center gap-2 px-4 py-2 rounded-[10px] border border-red-200 text-red-500 text-[13px] font-medium hover:bg-red-50 disabled:opacity-50 transition-all"
                    >
                        {isLeaving ? <Loader2 size={15} className="animate-spin" /> : <LogOut size={15} />}
                        Ieși din familie
                    </button>
                </div>
            </div>

            {isAdult && (
                <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 mb-8 stagger-2">
                    <h3 className="text-[14px] font-medium text-[#2D2926] mb-4 flex items-center gap-2">
                        <UserPlus size={18} className="text-[#C97B4B]" />
                        Invită un membru nou
                    </h3>
                    <form onSubmit={handleInviteMember} className="grid grid-cols-1 md:grid-cols-12 gap-3">
                        <div className="md:col-span-5 relative">
                            <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#9A8A7C]" size={16} />
                            <input
                                type="email"
                                value={inviteEmail}
                                onChange={(e) => setInviteEmail(e.target.value)}
                                placeholder="email@familie.com"
                                className="w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 pl-10 pr-4 text-[13px] focus:outline-none focus:border-[#C4B9AC]"
                                required
                            />
                        </div>
                        <div className="md:col-span-4">
                            <select
                                value={inviteRole}
                                onChange={(e) => setInviteRole(e.target.value as any)}
                                className="w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 px-4 text-[13px] text-[#2D2926] appearance-none cursor-pointer focus:outline-none"
                            >
                                <option value="Child">Rol: Copil</option>
                                <option value="Co-Parent">Rol: Co-Părinte</option>
                            </select>
                        </div>
                        <button
                            type="submit"
                            disabled={isAdding}
                            className="md:col-span-3 bg-[#2D2926] text-white px-4 py-2.5 rounded-[10px] text-[13px] font-medium hover:opacity-90 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
                        >
                            {isAdding
                                ? <><Loader2 size={14} className="animate-spin" /> Se trimite...</>
                                : addSuccess
                                    ? <><Check size={14} /> Invitație trimisă!</>
                                    : 'Trimite Invitație'}
                        </button>
                    </form>
                    {addError && <p className="mt-3 text-[12px] text-red-500">{addError}</p>}
                </div>
            )}

            <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden stagger-3">
                {loading ? (
                    <div className="p-10 flex justify-center items-center text-[#9A8A7C] gap-2">
                        <Loader2 size={18} className="animate-spin" /> Se încarcă membrii...
                    </div>
                ) : error ? (
                    <div className="p-10 text-center text-red-500 text-[13px]">{error}</div>
                ) : members.length === 0 ? (
                    <div className="p-10 text-center text-[#9A8A7C] text-[13px]">Nu există membri în familie.</div>
                ) : (
                    <div className="divide-y divide-[#EDE9E3]">
                        {members.map((member) => {
                            const isMe = member.userId === currentUserId;
                            const isChild = member.role === 'Child';
                            const bs = childBudgets[member.userId];

                            return (
                                <div key={member.id} className="p-6 hover:bg-[#FAF8F5]/40 transition-colors">
                                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                                        <div className="flex items-center gap-4">
                                            <div className="avatar-circle avatar-parent">
                                                {getRoleIcon(member.role)}
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2 mb-0.5">
                                                    <span className="text-[15px] font-medium text-[#2D2926]">{member.name}</span>
                                                    {isMe && (
                                                        <span className="px-2.5 py-1 bg-[#FFF8F2] text-[#C97B4B] border border-[#F0DFD0] rounded-[6px] text-[11px] font-medium">Tu</span>
                                                    )}
                                                    <span className="px-2.5 py-1 bg-emerald-50 text-emerald-700 border border-emerald-100 rounded-[6px] text-[11px] font-medium">Activ</span>
                                                </div>
                                                <div className="text-[12px] text-[#9A8A7C] flex items-center gap-2">
                                                    <span>{member.email}</span>
                                                    <span className="text-[#D4C9BC]">•</span>
                                                    <span className="font-medium uppercase tracking-wider text-[10px]">{member.role}</span>
                                                    {isChild && bs && bs.amount > 0 && (
                                                        <>
                                                            <span className="text-[#D4C9BC]">•</span>
                                                            <span className="text-[#C97B4B] font-medium">{Number(bs.amount).toFixed(0)} RON/lună</span>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                        {isAdult && !isMe && (
                                            <div className="flex items-center gap-2">
                                                {currentUserRole === 'Parent' && (
                                                    <div className="relative">
                                                        {roleChanging === member.id
                                                            ? <Loader2 size={14} className="animate-spin text-[#9A8A7C]" />
                                                            : (
                                                                <select
                                                                    value={member.role}
                                                                    onChange={(e) => handleRoleChange(member, e.target.value)}
                                                                    className="bg-[#FAF8F5] border border-[#EDE9E3] rounded-[8px] py-1.5 px-2 text-[12px] text-[#2D2926] appearance-none cursor-pointer focus:outline-none focus:border-[#C4B9AC] pr-6"
                                                                >
                                                                    <option value="Parent">Parent</option>
                                                                    <option value="Co-Parent">Co-Parent</option>
                                                                    <option value="Child">Child</option>
                                                                </select>
                                                            )
                                                        }
                                                    </div>
                                                )}
                                                <button
                                                    onClick={() => handleRemoveMember(member)}
                                                    className="text-red-500 hover:bg-red-50 p-2 rounded-lg transition-colors flex items-center gap-1.5 text-[13px] font-medium"
                                                    title="Elimină membru"
                                                >
                                                    <Trash2 size={16} /> <span className="sm:hidden">Elimină</span>
                                                </button>
                                            </div>
                                        )}
                                    </div>

                                    {/* Buget per copil — vizibil doar părinților */}
                                    {isAdult && isChild && (
                                        <form
                                            onSubmit={(e) => handleSetChildBudget(e, member.userId)}
                                            className="mt-4 ml-14 flex items-center gap-3"
                                        >
                                            <span className="text-[12px] text-[#9A8A7C] shrink-0">Buget lunar:</span>
                                            <input
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                value={bs?.input ?? ''}
                                                onChange={(e) => setChildBudgets(prev => ({
                                                    ...prev,
                                                    [member.userId]: { ...(prev[member.userId] ?? { amount: 0, saving: false, error: null, success: false }), input: e.target.value },
                                                }))}
                                                placeholder="Ex: 200.00"
                                                className="bg-[#FAF8F5] border border-[#EDE9E3] rounded-[8px] py-1.5 px-3 text-[13px] focus:outline-none focus:border-[#C4B9AC] w-32"
                                            />
                                            <span className="text-[12px] text-[#9A8A7C]">RON</span>
                                            <button
                                                type="submit"
                                                disabled={bs?.saving}
                                                className="bg-[#2D2926] text-white px-3 py-1.5 rounded-[8px] text-[12px] font-medium hover:opacity-90 disabled:opacity-50 transition-all flex items-center gap-1.5"
                                            >
                                                {bs?.saving
                                                    ? <Loader2 size={12} className="animate-spin" />
                                                    : bs?.success
                                                        ? <><Check size={12} /> Salvat</>
                                                        : 'Setează'}
                                            </button>
                                            {bs?.error && <span className="text-[11px] text-red-500">{bs.error}</span>}
                                        </form>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
