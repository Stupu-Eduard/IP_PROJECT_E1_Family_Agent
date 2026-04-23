import { useState } from 'react';
import * as yup from 'yup';
import { Link } from 'react-router-dom';

const resetSchema = yup.object().shape({
    email: yup.string().required('Adresa de email este obligatorie.').email('Email invalid.')
});

export default function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
    const [message, setMessage] = useState('');

    const handleReset = async (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('idle');
        setMessage('');

        try {
            await resetSchema.validate({ email });
            setStatus('loading');

            // Simulare request către backend
            await new Promise(resolve => setTimeout(resolve, 1500));

            setStatus('success');
            setMessage('Dacă adresa există în sistem, vei primi un link pentru resetarea parolei.');
        } catch (err: any) {
            setStatus('error');
            setMessage(err.message);
        }
    };

    const inputStyle = "w-full bg-white border border-[#EDE9E3] rounded-[10px] px-4 py-3 text-[13px] text-[#2D2926] focus:outline-none focus:border-[#C4B9AC] transition-colors";

    return (
        <div className="flex-1 w-full flex items-center justify-center p-6 min-h-screen bg-[#FAF8F5]">
            <div className="w-full max-w-md bg-white border border-[#EDE9E3] rounded-[14px] p-8 shadow-sm flex flex-col fade-in-up">
                <div className="flex flex-col items-center mb-8 text-center">
                    <div className="w-12 h-12 rounded-[10px] bg-[#2D2926] flex items-center justify-center text-white font-bold text-xl mb-4">FA</div>
                    <h1 className="text-[24px] font-medium text-[#2D2926]">Resetare Parolă</h1>
                    <p className="text-[13px] text-[#9A8A7C] mt-2">Introdu adresa de email pentru a primi instrucțiunile de recuperare.</p>
                </div>

                {status === 'error' && <div className="bg-[#FFF8F2] text-[#C97B4B] px-4 py-3 rounded-[10px] text-[13px] mb-6">⚠️ {message}</div>}
                {status === 'success' && <div className="bg-[#F5F8F5] text-[#4B7A4B] px-4 py-3 rounded-[10px] text-[13px] mb-6">✅ {message}</div>}

                <form onSubmit={handleReset} className="flex flex-col gap-4">
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className={inputStyle}
                        placeholder="adresa@exemplu.com"
                        disabled={status === 'success' || status === 'loading'}
                    />

                    <button
                        type="submit"
                        disabled={status === 'success' || status === 'loading'}
                        className="mt-2 w-full bg-[#2D2926] text-white rounded-[10px] py-3.5 text-[14px] font-medium shadow-sm transition-all hover:opacity-90 disabled:opacity-50"
                    >
                        {status === 'loading' ? 'Se procesează...' : 'Trimite Link'}
                    </button>
                </form>

                <div className="mt-8 text-center border-t border-[#EDE9E3] pt-6">
                    <p className="text-[13px] text-[#9A8A7C]">
                        <Link to="/login" className="font-medium text-[#2D2926] hover:text-[#C97B4B] transition-colors">
                            Înapoi la Autentificare
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}