
export interface UserData {
  id: number;
  name: string;
  email: string;
}

const API_URL = (import.meta as any).env.VITE_API_URL || 'https://api.family-agent.me';

export const getUserData = async (userId: number): Promise<UserData> => {
  const response = await fetch(`${API_URL}/users/${userId}`);
  
  if (!response.ok) {
    throw new Error('Eroare la preluarea datelor de la server');
  }
  
  return await response.json();
};