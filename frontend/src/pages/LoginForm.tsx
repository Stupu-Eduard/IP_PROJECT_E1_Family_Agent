import { useState } from 'react';
import * as yup from 'yup';
import { useAuthStore } from "../store/authStore";
// 1. Definim schema de validare Yup
const loginSchema = yup.object().shape({
  email: yup
      .string()
      .required('Adresa de email este obligatorie.')
      .email('Te rugăm să introduci o adresă de email validă.'),
  password: yup
      .string()
      .required('Parola este obligatorie.')
      .min(6, 'Parola trebuie să aibă minimum 6 caractere.')
});

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // 2. Aducem funcția login din state-ul global (Zustand)
  const login = useAuthStore((state) => state.login);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      // 3. Validăm datele introduse
      await loginSchema.validate({ email, password });
      setIsLoading(true);

      // 4. Simulăm un apel API Fetch/Axios către endpoint-ul POST /auth/login
      const mockApiCall = new Promise<{ token: string }>((resolve, reject) => {
        setTimeout(() => {
          // Aici ar fi verificarea de pe backend. Simulăm un succes general:
          if (email && password) {
            resolve({ token: "jwt_token_simulat_premium_987654321" });
          } else {
            reject(new Error("Eroare de la server: Date incorecte."));
          }
        }, 1500); // 1.5 secunde delay pentru a vedea starea de loading
      });

      // Așteptăm răspunsul de la "server"
      const response = await mockApiCall;

      // 5. Salvăm token-ul în LocalStorage și actualizăm State-ul Global
      login(response.token);

      alert('Autentificare reușită! Token JWT salvat și state global actualizat. ✅');

      // Aici poți adăuga redirect-ul folosind useNavigate() din react-router-dom
      // ex: navigate('/dashboard');

    } catch (err: unknown) {
      if (err instanceof yup.ValidationError) {
        setError(err.message); // Eroare de la validarea Yup
      } else if (err instanceof Error) {
        setError(err.message); // Eroare simulată de la backend
      } else {
        setError('A apărut o eroare neașteptată.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
      <div className="min-h-screen flex items-center justify-center bg-[#fdfdfc] p-6">

        <div className="max-w-md w-full bg-white rounded-3xl shadow-[0_20px_60px_-15px_rgba(102,130,102,0.15)] p-12 border border-gray-100 animate-fade-in-up">

          {/* Secțiunea de Header */}
          <div className="text-center mb-10">
            <div className="mx-auto w-16 h-16 bg-sage-100 rounded-3xl flex items-center justify-center mb-5 shadow-inner border border-sage-200">
              <svg
                  viewBox="0 0 24 24"
                  className="w-8 h-8 text-sage-600"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M9 22V8" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M15 22V8" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M9 10L12 7L15 10" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M12 7V2" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M6 16H18" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <h2 className="text-3xl font-extrabold text-gray-950 tracking-tight">Bine ai venit</h2>
            <p className="text-gray-600 mt-2 font-medium">Introdu datele pentru a accesa contul</p>
          </div>

          {/* Afișare Erori */}
          {error && (
              <div className="mb-6 p-4 bg-red-50 text-red-800 rounded-2xl text-sm border border-red-200 font-medium flex items-center gap-3 animate-[head-shake_0.4s_ease-in-out]">
                <span className="text-xl">⚠️</span>
                {error}
              </div>
          )}

          <form onSubmit={handleLogin} className="space-y-6">
            <div className="group">
              <label className="block text-sm font-semibold text-gray-800 mb-1.5 ml-1">Email</label>
              <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-5 py-3.5 border border-gray-200 rounded-2xl transition-all duration-200 focus:border-sage-400 focus:ring-4 focus:ring-sage-100 focus:outline-none placeholder:text-gray-400 group-hover:border-gray-300"
                  placeholder="nume@exemplu.com"
              />
            </div>

            <div className="group">
              <div className="flex items-center justify-between mb-1.5 ml-1">
                <label className="block text-sm font-semibold text-gray-800">Parolă</label>
                <a href="#" className="text-sm text-sage-600 hover:text-sage-700 font-semibold transition-colors">Ai uitat parola?</a>
              </div>
              <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full px-5 py-3.5 border border-gray-200 rounded-2xl transition-all duration-200 focus:border-sage-400 focus:ring-4 focus:ring-sage-100 focus:outline-none placeholder:text-gray-400 group-hover:border-gray-300"
                  placeholder="••••••••"
              />
            </div>

            <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-sage-600 text-white font-bold py-4 px-6 rounded-2xl shadow-lg shadow-sage-500/20 transition-all duration-300 transform hover:bg-sage-700 hover:shadow-xl hover:shadow-sage-600/30 hover:-translate-y-0.5 active:scale-[0.98] disabled:bg-sage-300 flex items-center justify-center gap-3"
            >
              {isLoading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                    Se procesează...
                  </>
              ) : 'Intră în cont'}
            </button>
          </form>

          <div className="mt-10 text-center border-t border-gray-100 pt-8">
            <p className="text-gray-600 font-medium">
              Nu ai cont încă? {' '}
              <a href="#" className="text-sage-600 hover:text-sage-700 font-semibold transition-colors">Creează unul gratuit</a>
            </p>
          </div>
        </div>
      </div>
  );
}