export interface GroupMemberDTO {
    id: number;
    userId: number;
    name: string;
    email: string;
    /**
     * Rolul membrului în familie.
     * Nota: backend-ul normalizează "Child-PendingAdult" → "Child" în GET /members,
     * dar îl returnează ca "Child-PendingAdult" în GET /adult-requests.
     */
    role: 'Parent' | 'Co-Parent' | 'Child' | 'Child-PendingAdult';
}