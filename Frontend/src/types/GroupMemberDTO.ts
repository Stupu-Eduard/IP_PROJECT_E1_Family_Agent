export interface GroupMemberDTO {
    id: number;
    userId: number;
    name: string;
    email: string;
    role: 'Parent' | 'Co-Parent' | 'Adult' | 'Child';
    owner: boolean;
}
