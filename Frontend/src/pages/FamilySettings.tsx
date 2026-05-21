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

interface JwtPayload {
    role?: string;
    userId?: number;
    familyId?: number;
    [key: string]: unknown;
}

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

    const payload = token ? decodeJwtPayload(token) as JwtPayload : null;
    const currentUserRole = payload?.role ?? 'Child';
    const currentUserId: number = payload?.userId ?? 0;
    const familyId: number | null = payload?.familyId ?? null;
    const isAdult = currentUserRole === 'Parent' || currentUserRole === 'Co-Parent';
    const isChild = currentUserRole === 'Child';

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
    const [deletingChildId, setDeletingChildId] = useState<number | null>(null);

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
        } catch (err: unknown) {
            const status = (err as { response?: { status?: number } })?.response?.status;
            if (status === 403) {
                try {
                    const { data } = await authApi.refresh();
                    useAuthStore.getState().setToken(data.token);
                    window.location.reload();
                } catch {
                    useAuthStore.getState().logout();
                    window.location.replace('/login');
                }
            } else {
                setError('Nu s-au putut incarca membrii familiei.');
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

    const getErrMsg = (err: unknown, fallback: string): string => {
        const data = (err as { response?: { data?: unknown } })?.response?.data;
        if (typeof data === 'string') return data;
        if (data && typeof (data as { message?: unknown }).message === 'string') return (data as { message: string }).message;
        return fallback;
    };

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
        } catch (err: unknown) {
            setAddError(getErrMsg(err, 'Eroare la trimiterea invitatiei.'));
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
        } catch (err: unknown) {
            setCreateError(getErrMsg(err, 'Eroare la crearea familiei.'));
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
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la acceptarea invitatiei.'));
        } finally {
            setInvActionId(null);
        }
    };

    const handleDeclineInvitation = async (inv: InvitationDTO) => {
        setInvActionId(inv.id);
        try {
            await invitationApi.decline(inv.id);
            setPendingInvitations(prev => prev.filter(i => i.id !== inv.id));
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la refuzarea invitatiei.'));
        } finally {
            setInvActionId(null);
        }
    };

    const handleLeaveFamily = async () => {
        if (!familyId) return;
        if (!window.confirm('Esti sigur ca vrei sa iesi din familie?')) return;
        setIsLeaving(true);
        try {
            await familyApi.leaveFamily(familyId);
            const { data } = await authApi.refresh();
            useAuthStore.getState().setToken(data.token);
            window.location.reload();
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la iesirea din familie.'));
        } finally {
            setIsLeaving(false);
        }
    };

    const handleDeleteFamily = async () => {
        if (!familyId) return;
        if (!window.confirm('Esti sigur ca vrei sa stergi familia? Aceasta actiune este ireversibila.')) return;
        setIsDeleting(true);
        try {
            await familyApi.deleteFamily(familyId);
            const { data } = await authApi.refresh();
            useAuthStore.getState().setToken(data.token);
            window.location.reload();
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la stergerea familiei.'));
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
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la schimbarea rolului.'));
        } finally {
            setRoleChanging(null);
        }
    };

    const handleRemoveMember = async (member: GroupMemberDTO) => {
        if (!familyId) return;
        const label = member.name || member.email;
        if (!window.confirm(`Esti sigur ca doresti sa elimini membrul ${label}?`)) return;
        try {
            await familyApi.removeMember(familyId, member.id);
            setMembers(prev => prev.filter(m => m.id !== member.id));
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la eliminarea membrului.'));
        }
    };

    const handleDeleteChildAccount = async (member: GroupMemberDTO) => {
        if (!familyId) return;
        const label = member.name || member.email;
        if (!window.confirm(`Esti sigur ca vrei sa stergi COMPLET contul lui ${label}? Aceasta actiune este ireversibila.`)) return;
        setDeletingChildId(member.id);
        try {
            await familyApi.deleteChildAccount(familyId, member.id);
            setMembers(prev => prev.filter(m => m.id !== member.id));
        } catch (err: unknown) {
            alert(getErrMsg(err, 'Eroare la stergerea contului.'));
        } finally {
            setDeletingChildId(null);
        }
    };

    const handleSetChildBudget = async (e: React.FormEvent, childUserId: number) => {
        e.preventDefault();
        const state = childBudgets[childUserId];
        const amount = parseFloat(state?.input ?? '');
        if (isNaN(amount) || amount < 0) {
            setChildBudgets(prev => ({ ...prev, [childUserId]: { ...prev[childUserId], error: 'Suma invalida.' } }));
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
        } catch (err: unknown) {
            setChildBudgets(prev => ({
                ...prev,
                [childUserId]: { ...prev[childUserId], saving: false, error: getErrMsg(err, 'Eroare la salvare.') },
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

                {!isChild && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 mb-6">
                        <h3 className="text-[14px] font-medium text-[#2D2926] mb-4 flex items-center gap-2">
                            <UserPlus size={18} className="text-[#C97B4B]" />
                            Creeaza o familie noua
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
                                {isCreating ? 'Se creeaza...' : 'Creeaza'}
                            </button>
                        </form>
                        {createError && <p className="mt-3 text-[12px] text-red-500">{createError}</p>}
                    </div>
                )}

                {isChild && pendingInvitations.length === 0 && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-8 text-center">
                        <Bell size={28} className="text-[#C4B9AC] mx-auto mb-3" />
                        <p className="text-[14px] font-medium text-[#2D2926] mb-1">Nicio invitatie in asteptare</p>
                        <p className="text-[12px] text-[#9A8A7C]">Cere unui parinte sa te invite prin email.</p>
                    </div>
                )}

                {pendingInvitations.length > 0 && (
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden">
                        <div className="px-6 py-4 border-b border-[#EDE9E3] flex items-center gap-2">
                            <Bell size={16} className="text-[#C97B4B]" />
                            <span className="text-[14px] font-medium text-[#2D2926]">Invitatii in asteptare</span>
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
                                        Accepta
                                    </button>
                                    <button
                                        disabled={invActionId === inv.id}
                                        onClick={() => handleDeclineInvitation(inv)}
                                        className="bg-white border border-[#EDE9E3] text-[#2D2926] px-4 py-2 rounded-[10px] text-[13px] font-medium hover:border-[#C4B9AC] disabled:opacity-50"
                                    >
                                        Refuza
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
                        {isAdult ? 'Administreaza rolurile, accesul si bugetele membrilor' : 'Vezi cine mai face parte din grupul tau'}
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
                            Sterge familia
                        </button>
                    )}
                    <button
                        onClick={handleLeaveFamily}
                        disabled={isLeaving}
                        className="flex items-center gap-2 px-4 py-2 rounded-[10px] border border-red-200 text-red-500 text-[13px] font-medium hover:bg-red-50 disabled:opacity-50 transition-all"
                    >
                        {isLeaving ? <Loader2 size={15} className="animate-spin" /> : <LogOut size={15} />}
                        Iesi din familie
                    </button>
                </div>
            </div>

            {isAdult && (
                <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 mb-8 stagger-2">
                    <h3 className="text-[14px] font-medium text-[#2D2926] mb-4 flex items-center gap-2">
                        <UserPlus size={18} className="text-[#C97B4B]" />
                        Invita un membru nou
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
                                onChange={(e) => setInviteRole(e.target.value as 'Co-Parent' | 'Child')}
                                className="w-full bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] py-2.5 px-4 text-[13px] text-[#2D2926] appearance-none cursor-pointer focus:outline-none"
                            >
                                <option value="Child">Rol: Copil</option>
                                <option value="Co-Parent">Rol: Co-Parinte</option>
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
                                    ? <><Check size={14} /> Trimis!</>
                                    : 'Trimite Invitatie'}
                        </button>
                    </form>
                    {addError && <p className="mt-3 text-[12px] text-red-500">{addError}</p>}
                </div>
            )}

            <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden stagger-3">
                {loading ? (
                    <div className="p-10 flex justify-center items-center text-[#9A8A7C] gap-2">
                        <Loader2 size={18} className="animate-spin" /> Se incarca membrii...
                    </div>
                ) : error ? (
                    <div className="p-10 text-center text-red-500 text-[13px]">{error}</div>
                ) : members.length === 0 ? (
                    <div className="p-10 text-center text-[#9A8A7C] text-[13px]">Nu exista membri in familie.</div>
                ) : (
                    <div className="divide-y divide-[#EDE9E3]">
                        {members.map((member) => {
                            const isMe = member.userId === currentUserId;
                            const isMemberChild = member.role === 'Child';
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
                                                    {isMemberChild && bs && bs.amount > 0 && (
                                                        <>
                                                            <span className="text-[#D4C9BC]">•</span>
                                                            <span className="text-[#C97B4B] font-medium">{Number(bs.amount).toFixed(0)} RON/luna</span>
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
                                                    className="text-red-500 hover:bg-red-50 p-2 rounded-lg transition-colors"
                                                    title="Elimina din familie"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                                {isMemberChild && (
                                                    <button
                                                        onClick={() => handleDeleteChildAccount(member)}
                                                        disabled={deletingChildId === member.id}
                                                        className="text-red-700 hover:bg-red-100 border border-red-200 px-2.5 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 text-[12px] font-medium disabled:opacity-50"
                                                        title="Sterge contul complet"
                                                    >
                                                        {deletingChildId === member.id
                                                            ? <Loader2 size={13} className="animate-spin" />
                                                            : <Trash2 size={13} />}
                                                        Sterge cont
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>

                                    {isAdult && isMemberChild && (
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
                                                        : 'Seteaza'}
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
