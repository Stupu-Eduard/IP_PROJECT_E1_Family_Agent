import { decodeJwtPayload } from '../utils/jwt';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import type { GroupMemberDTO } from "../types/GroupMemberDTO.ts";
import {
    Mail, UserPlus, Trash2, Shield,
    ArrowLeft, Baby, Crown
} from 'lucide-react'; // Am eliminat 'Users' și 'User' care nu erau folosite

export default function FamilySettings() {
    const token = useAuthStore((state) => state.token);
    const navigate = useNavigate();

    const payload = token ? decodeJwtPayload(token) : null;

    const currentUserRole = (payload as any)?.role || 'Child';

    const isAdult = currentUserRole === 'Parent' || currentUserRole === 'Co-Parent';

    const [members, setMembers] = useState<GroupMemberDTO[]>( [
        { id: '1', name: 'Edi (Tu)', email: 'eduard@parent.com', role: 'Parent', status: 'Accepted' },
        { id: '2', name: 'Mihaela ', email: 'mihaela@partner.com', role: 'Co-Parent', status: 'Accepted' },
        { id: '3', name: 'Andrei', email: 'andrei@kid.com', role: 'Child', status: 'Accepted' },
        { id: '4', name: '-', email: 'invitat@exemplu.com', role: 'Child', status: 'Pending' },
    ]);

    const [inviteEmail, setInviteEmail] = useState('');
    const [inviteRole, setInviteRole] = useState<'Co-Parent' | 'Child'>('Child');
    const [isInviting, setIsInviting] = useState(false);

    const handleInvite = (e: React.FormEvent) => {
        e.preventDefault();
        if (!inviteEmail.trim()) return;

        setIsInviting(true);
        setTimeout(() => {

            const newMember = {
                id: Date.now().toString(),
                name: '-',
                email: inviteEmail,
                role: inviteRole as string,
                status: 'Pending'
            } as any;

            setMembers([...members, newMember]);
            setInviteEmail('');
            setIsInviting(false);
        }, 1000);
    };

    const handleRemoveMember = (id: string, name: string) => {
        const confirmDelete = window.confirm(`Ești sigur că dorești să elimini membrul ${name !== '-' ? name : 'invitat'}?`);
        if (confirmDelete) {
            setMembers(members.filter(m => m.id !== id));
        }
    };

    const getStatusBadge = (status: string) => {
        if (status === 'Accepted') return <span className="px-2.5 py-1 bg-emerald-50 text-emerald-700 border border-emerald-100 rounded-[6px] text-[11px] font-medium">Activ</span>;
        return <span className="px-2.5 py-1 bg-[#FFF8F2] text-[#C97B4B] border border-[#F0DFD0] rounded-[6px] text-[11px] font-medium">Invitație Trimisă</span>;
    };

    const getRoleIcon = (role: string) => {
        if (role === 'Parent') return <Crown size={18} className="text-[#C97B4B]" />;
        if (role === 'Co-Parent') return <Shield size={18} className="text-[#B5956A]" />;
        return <Baby size={18} className="text-[#8C7E6E]" />;
    };

    return (
        <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[800px] mx-auto w-full flex-1">
            <div className="flex items-center gap-4 mb-8 stagger-1">
                <button onClick={() => navigate('/dashboard')} className="btn-alive-secondary !p-0 w-10 h-10 justify-center shrink-0">
                    <ArrowLeft size={18} />
                </button>
                <div>
                    <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight">
                        {isAdult ? 'Gestionare Familie' : 'Membrii Familiei Mele'}
                    </h2>
                    <p className="text-[13px] text-[#9A8A7C]">
                        {isAdult ? 'Administrează rolurile și accesul membrilor' : 'Vezi cine mai face parte din grupul tău'}
                    </p>
                </div>
            </div>

            {isAdult && (
                <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 mb-8 stagger-2">
                    <h3 className="text-[14px] font-medium text-[#2D2926] mb-4 flex items-center gap-2">
                        <UserPlus size={18} className="text-[#C97B4B]" />
                        Adaugă un membru nou
                    </h3>
                    <form onSubmit={handleInvite} className="grid grid-cols-1 md:grid-cols-12 gap-3">
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
                                <option value="Child">Rol: Copil </option>
                                <option value="Co-Parent">Rol: Co-Părinte</option>
                            </select>
                        </div>
                        <button
                            type="submit"
                            disabled={isInviting}
                            className="md:col-span-3 bg-[#2D2926] text-white px-4 py-2.5 rounded-[10px] text-[13px] font-medium hover:opacity-90 disabled:opacity-50 transition-all"
                        >
                            {isInviting ? 'Se trimite...' : 'Trimite Invitație'}
                        </button>
                    </form>
                </div>
            )}

            <div className="bg-white border border-[#EDE9E3] rounded-[14px] overflow-hidden stagger-3">
                <div className="divide-y divide-[#EDE9E3]">
                    {members.map((member) => (
                        <div key={member.id} className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-[#FAF8F5]/40 transition-colors">
                            <div className="flex items-center gap-4">
                                <div className="avatar-circle avatar-parent">
                                    {getRoleIcon(member.role)}
                                </div>
                                <div>
                                    <div className="flex items-center gap-2 mb-0.5">
                                        <span className="text-[15px] font-medium text-[#2D2926]">{member.name !== '-' ? member.name : member.email}</span>
                                        {getStatusBadge(member.status)}
                                    </div>
                                    <div className="text-[12px] text-[#9A8A7C] flex items-center gap-2">
                                        <span>{member.email}</span>
                                        <span className="text-[#D4C9BC]">•</span>
                                        <span className="font-medium uppercase tracking-wider text-[10px]">{member.role}</span>
                                    </div>
                                </div>
                            </div>
                            {isAdult && member.id !== '1' && (
                                <button
                                    onClick={() => handleRemoveMember(member.id, member.name)}
                                    className="text-red-500 hover:bg-red-50 p-2 rounded-lg transition-colors flex items-center gap-1.5 text-[13px] font-medium"
                                    title="Elimină membru"
                                >
                                    <Trash2 size={16} /> <span className="sm:hidden">Elimină</span>
                                </button>
                            )}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}