import { useState } from 'react';
import * as yup from 'yup';
import { Navigate, useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from "../store/authStore";
import { isTokenExpired } from '../utils/jwt';
import type { RegisterDTO } from '../types/AuthDTO';

const registerSchema = yup.object().shape({
    name: yup.string().required('Numele este obligatoriu.').min(2, 'Minim 2 caractere.'),
    email: yup.string().required('Email obligatoriu.').email('Email invalid.'),
    password: yup.string().required('Parola este obligatorie.').min(8, 'Minim 8 caractere.'),
    confirmPassword: yup.string().required('Confirmă parola.').oneOf([yup.ref('password')], 'Parolele nu coincid.')
});

export default function RegisterForm() {
    const navigate = useNavigate();
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const token = useAuthStore((state) => state.token);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const login = useAuthStore((state) => state.login);

    if (isAuthenticated && token && !isTokenExpired(token)) {
        return <Navigate to="/dashboard" replace />;
    }

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        try {
            await registerSchema.validate({ name, email, password, confirmPassword });
            setIsLoading(true);

            const payload: RegisterDTO = { name, email, password };

            // Simulare API Call
            const mockApiCall = new Promise<{ token: string }>((resolve, reject) => {
                setTimeout(() => {
                    if (payload.email === 'test@example.com' || payload.email === 'copil@example.com') {
                        reject(new Error("Eroare 409: Acest email este deja asociat unui cont."));
                    } else {
                        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
                        const payloadData = btoa(JSON.stringify({
                            sub: payload.email,
                            role: 'Parent',
                            exp: Math.floor(Date.now() / 1000) + 60 * 60
                        }));
                        resolve({ token: `${header}.${payloadData}.mock_signature` });
                    }
                }, 1500);
            });

            const response = await mockApiCall;
            login(response.token);
            navigate('/dashboard', { replace: true });

        } catch (err: unknown) {
            if (err instanceof yup.ValidationError) {
                setError(err.message);
            } else if (err instanceof Error) {
                setError(err.message);
            } else {
                setError('Eroare neașteptată de rețea.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors";

    return (
        <div className="flex-1 w-full flex items-center justify-center p-6 bg-[#FAF8F5] min-h-screen">
            <div className="w-full max-w-md bg-white border border-[#EDE9E3] rounded-[14px] p-8 shadow-sm flex flex-col fade-in-up">
                <div className="flex flex-col items-center mb-8 text-center">
                    <div className="w-12 h-12 rounded-[10px] bg-[#2D2926] flex items-center justify-center text-white font-bold text-xl mb-4 shadow-sm">FA</div>
                    <h1 className="text-[24px] font-medium text-[#2D2926] tracking-tight mb-1">Creează Cont Nou</h1>
                    <p className="text-[13px] text-[#9A8A7C]">Alătură-te platformei FamilyAgent.</p>
                </div>

                {error && (
                    <div className="bg-[#FFF8F2] border border-[#F0DFD0] text-[#C97B4B] px-4 py-3 rounded-[10px] text-[13px] font-medium mb-6 flex items-start gap-2">
                        <span className="mt-0.5">⚠️</span><span>{error}</span>
                    </div>
                )}

                <form onSubmit={handleRegister} className="flex flex-col gap-4">
                    <input type="text" value={name} onChange={(e) => setName(e.target.value)} className={inputStyle} placeholder="Nume Complet" />
                    <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputStyle} placeholder="Email" />
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={inputStyle} placeholder="Parolă (Min. 8 caractere)" />
                    <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className={inputStyle} placeholder="Confirmare Parolă" />

                    <button type="submit" disabled={isLoading} className="mt-4 w-full bg-[#2D2926] text-white rounded-[10px] py-3.5 text-[14px] font-medium shadow-sm transition-all hover:opacity-90 disabled:opacity-50">
                        {isLoading ? 'Se procesează...' : 'Creează Contul'}
                    </button>
                </form>

                <div className="mt-8 text-center border-t border-[#EDE9E3] pt-6">
                    <p className="text-[13px] text-[#9A8A7C]">
                        Ai deja un cont? <Link to="/login" className="font-medium text-[#2D2926] hover:text-[#C97B4B] transition-colors">Autentifică-te aici</Link>
                    </p>
                </div>
            </div>
        </div>
    );
}