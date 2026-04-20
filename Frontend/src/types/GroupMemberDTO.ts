export interface GroupMemberDTO {
    id: string;
    name: string;
    email: string;
    role: 'Parent' | 'Co-Parent' | 'Child';
    status: 'Accepted' | 'Pending';
}