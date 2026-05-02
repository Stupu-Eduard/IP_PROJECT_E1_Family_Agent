import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

const features = [
    {
        icon: '📊',
        title: 'Rapoarte Inteligente',
        desc: 'Vizualizează cheltuielile lunare cu grafice clare și intuitive. Înțelege unde se duc banii familiei tale.',
    },
    {
        icon: '👨‍👩‍👧‍👦',
        title: 'Conturi Familie',
        desc: 'Roluri separate pentru părinți și copii. Fiecare membru are accesul potrivit, adaptat nevoilor sale.',
    },
    {
        icon: '📍',
        title: 'Localizare Live',
        desc: 'Știi mereu unde sunt membrii familiei tale prin harta interactivă și alertele de geofencing.',
    },
    {
        icon: '🤖',
        title: 'Asistent AI',
        desc: 'Asistentul tău personal de buget răspunde la întrebări și oferă recomandări financiare personalizate.',
    },
    {
        icon: '🧾',
        title: 'Scanare Bonuri',
        desc: 'Fotografiază bonul fiscal și AI-ul extrage automat cheltuiala. Nu mai introduci date manual.',
    },
    {
        icon: '🔒',
        title: 'Securitate Maximă',
        desc: 'Autentificare sigură cu JWT. Datele tale financiare sunt protejate și private.',
    },
];

const stats = [
    { value: '100%', label: 'Gratuit', sub: 'fără costuri ascunse' },
    { value: '∞', label: 'Cheltuieli', sub: 'înregistrate nelimitat' },
    { value: '24/7', label: 'Asistent AI', sub: 'mereu disponibil' },
];

function FloatingOrb({ className }: { className: string }) {
    return <div className={`absolute rounded-full blur-3xl opacity-20 animate-float ${className}`} />;
}

export default function LandingPage() {
    const [scrollY, setScrollY] = useState(0);
    const heroRef = useRef<HTMLDivElement>(null);
    const [visible, setVisible] = useState<Set<number>>(new Set());
    const featureRefs = useRef<(HTMLDivElement | null)[]>([]);

    useEffect(() => {
        const handleScroll = () => setScrollY(window.scrollY);
        window.addEventListener('scroll', handleScroll, { passive: true });
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        const idx = parseInt((entry.target as HTMLElement).dataset.idx || '0');
                        setVisible((prev) => new Set([...prev, idx]));
                    }
                });
            },
            { threshold: 0.15 }
        );
        featureRefs.current.forEach((el) => el && observer.observe(el));
        return () => observer.disconnect();
    }, []);

    return (
        <div className="min-h-screen w-full overflow-x-hidden bg-[#FAF8F5] font-sans text-[#2D2926]">

            {/* ── NAV ── */}
            <nav
                className="fixed top-0 left-0 right-0 z-50 transition-all duration-300"
                style={{
                    background: scrollY > 40 ? 'rgba(250,248,245,0.92)' : 'transparent',
                    backdropFilter: scrollY > 40 ? 'blur(12px)' : 'none',
                    borderBottom: scrollY > 40 ? '1px solid #EDE9E3' : '1px solid transparent',
                }}
            >
                <div className="max-w-[1100px] mx-auto px-6 py-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-[8px] bg-[#2D2926] flex items-center justify-center text-white font-bold text-sm shadow-sm">
                            FA
                        </div>
                        <span className="font-semibold text-[#2D2926] text-[15px] tracking-tight">FamilyAgent</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <Link
                            to="/login"
                            className="px-4 py-2 text-[13px] font-medium text-[#9A8A7C] hover:text-[#2D2926] transition-colors"
                        >
                            Intră în cont
                        </Link>
                        <Link
                            to="/register"
                            className="px-4 py-2 text-[13px] font-medium bg-[#2D2926] text-white rounded-[8px] hover:bg-[#C97B4B] transition-all duration-300 shadow-sm"
                        >
                            Începe gratuit →
                        </Link>
                    </div>
                </div>
            </nav>

            {/* ── HERO ── */}
            <section
                ref={heroRef}
                className="relative min-h-screen flex items-center justify-center overflow-hidden"
            >
                {/* Background orbs */}
                <FloatingOrb className="w-[600px] h-[600px] bg-[#C97B4B] top-[-10%] right-[-15%] animation-delay-0" />
                <FloatingOrb className="w-[400px] h-[400px] bg-[#9A8A7C] bottom-[-5%] left-[-10%] animation-delay-2000" />
                <FloatingOrb className="w-[300px] h-[300px] bg-[#C97B4B] top-[30%] left-[10%] animation-delay-4000" />

                {/* Grain texture overlay */}
                <div className="absolute inset-0 grain-overlay opacity-40 pointer-events-none" />

                {/* Geometric accent lines */}
                <div className="absolute top-[20%] right-[8%] w-px h-[180px] bg-gradient-to-b from-transparent via-[#C97B4B] to-transparent opacity-30" />
                <div className="absolute bottom-[25%] left-[5%] w-[120px] h-px bg-gradient-to-r from-transparent via-[#9A8A7C] to-transparent opacity-30" />

                <div className="relative z-10 max-w-[860px] mx-auto px-6 pt-28 pb-20 text-center">
                    {/* Badge */}
                    <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-[#EDE9E3] bg-white/70 backdrop-blur-sm text-[12px] font-medium text-[#9A8A7C] mb-8 animate-fade-in-down shadow-sm">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#C97B4B] animate-pulse" />
                        Aplicație de gestiune financiară pentru familie
                    </div>

                    {/* Headline */}
                    <h1 className="text-[52px] md:text-[72px] font-semibold tracking-[-3px] leading-[1.05] text-[#2D2926] mb-6 animate-hero-title">
                        Finanțele familiei,{' '}
                        <span className="relative inline-block">
              <span className="relative z-10 text-[#C97B4B]">simplificate.</span>
              <svg
                  className="absolute -bottom-2 left-0 w-full animate-draw-line"
                  height="8"
                  viewBox="0 0 300 8"
                  preserveAspectRatio="none"
              >
                <path
                    d="M0 6 Q75 0 150 4 Q225 8 300 3"
                    stroke="#C97B4B"
                    strokeWidth="2"
                    fill="none"
                    strokeLinecap="round"
                    opacity="0.4"
                />
              </svg>
            </span>
                    </h1>

                    <p className="text-[17px] text-[#9A8A7C] leading-[1.7] max-w-[560px] mx-auto mb-10 animate-fade-in-up-delay">
                        Gestionează cheltuielile, urmărește membrii familiei în timp real și primește
                        sfaturi financiare de la un asistent AI — totul într-un singur loc.
                    </p>

                    {/* CTAs */}
                    <div className="flex items-center justify-center gap-4 flex-wrap animate-fade-in-up-delay-2">
                        <Link
                            to="/register"
                            className="group px-8 py-4 bg-[#2D2926] text-white rounded-[12px] text-[15px] font-medium hover:bg-[#C97B4B] transition-all duration-300 shadow-lg hover:shadow-xl hover:-translate-y-1 flex items-center gap-2"
                        >
                            Creează cont gratuit
                            <span className="group-hover:translate-x-1 transition-transform duration-200">→</span>
                        </Link>
                        <Link
                            to="/login"
                            className="px-8 py-4 border border-[#EDE9E3] bg-white/60 backdrop-blur-sm rounded-[12px] text-[15px] font-medium text-[#2D2926] hover:border-[#C97B4B] hover:bg-white transition-all duration-300 shadow-sm"
                        >
                            Am deja cont
                        </Link>
                    </div>

                    {/* Mock UI Card */}
                    <div className="mt-16 relative mx-auto max-w-[720px] animate-float-card">
                        <div className="bg-white/80 backdrop-blur-md rounded-[20px] border border-[#EDE9E3] shadow-[0_20px_80px_rgba(45,41,38,0.10)] p-6 text-left">
                            <div className="flex items-center justify-between mb-5">
                                <div>
                                    <div className="text-[10px] tracking-[1.2px] text-[#B8A99A] font-medium mb-1">SUMAR LUNA CURENTĂ</div>
                                    <div className="text-[28px] font-semibold text-[#2D2926] tracking-[-1px]">3.240 RON</div>
                                </div>
                                <div className="flex gap-2">
                                    {['#C97B4B', '#9A8A7C', '#EDE9E3'].map((c, i) => (
                                        <div key={i} className="w-8 h-8 rounded-full border-2 border-white shadow-sm" style={{ background: c }} />
                                    ))}
                                </div>
                            </div>
                            <div className="grid grid-cols-3 gap-3">
                                {[
                                    { label: 'Alimente', val: '1.100', color: '#C97B4B', pct: 34 },
                                    { label: 'Transport', val: '620', color: '#9A8A7C', pct: 19 },
                                    { label: 'Utilități', val: '480', color: '#B8A99A', pct: 15 },
                                ].map((item) => (
                                    <div key={item.label} className="bg-[#FAF8F5] rounded-[12px] p-3">
                                        <div className="text-[10px] text-[#B8A99A] tracking-[0.6px] mb-1">{item.label.toUpperCase()}</div>
                                        <div className="text-[18px] font-semibold text-[#2D2926] mb-2">{item.val} RON</div>
                                        <div className="h-1.5 bg-[#EDE9E3] rounded-full overflow-hidden">
                                            <div
                                                className="h-full rounded-full animate-bar-grow"
                                                style={{ width: `${item.pct}%`, background: item.color }}
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                        {/* Floating notification badge */}
                        <div className="absolute -top-4 -right-4 bg-white border border-[#EDE9E3] rounded-[12px] px-3 py-2 shadow-lg flex items-center gap-2 animate-float-badge">
                            <span className="text-lg">🤖</span>
                            <div>
                                <div className="text-[11px] font-medium text-[#2D2926]">Sfat AI</div>
                                <div className="text-[10px] text-[#9A8A7C]">Economisești 15% la utilități!</div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Scroll indicator */}
                <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 animate-fade-in-up-delay-3">
                    <span className="text-[11px] tracking-[1px] text-[#B8A99A]">DESCOPERĂ</span>
                    <div className="w-px h-8 bg-gradient-to-b from-[#B8A99A] to-transparent animate-scroll-line" />
                </div>
            </section>

            {/* ── STATS ── */}
            <section className="py-16 border-y border-[#EDE9E3] bg-white">
                <div className="max-w-[860px] mx-auto px-6 grid grid-cols-3 gap-8">
                    {stats.map((s) => (
                        <div key={s.label} className="text-center">
                            <div className="text-[40px] font-bold text-[#C97B4B] tracking-[-2px] leading-none mb-1">{s.value}</div>
                            <div className="text-[14px] font-medium text-[#2D2926]">{s.label}</div>
                            <div className="text-[12px] text-[#B8A99A] mt-0.5">{s.sub}</div>
                        </div>
                    ))}
                </div>
            </section>

            {/* ── FEATURES ── */}
            <section className="py-24 max-w-[1060px] mx-auto px-6">
                <div className="text-center mb-16">
                    <div className="text-[11px] tracking-[2px] text-[#B8A99A] font-medium mb-3">FUNCȚIONALITĂȚI</div>
                    <h2 className="text-[38px] font-semibold tracking-[-1.5px] text-[#2D2926] leading-tight">
                        Tot ce ai nevoie,<br />
                        <span className="text-[#C97B4B]">într-o singură aplicație.</span>
                    </h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {features.map((f, i) => (
                        <div
                            key={i}
                            ref={(el) => { featureRefs.current[i] = el; }}
                            data-idx={i}
                            className="group bg-white border border-[#EDE9E3] rounded-[16px] p-6 hover:border-[#C97B4B] hover:-translate-y-1 hover:shadow-lg transition-all duration-300"
                            style={{
                                opacity: visible.has(i) ? 1 : 0,
                                transform: visible.has(i) ? 'translateY(0)' : 'translateY(24px)',
                                transition: `opacity 0.5s ease ${i * 80}ms, transform 0.5s ease ${i * 80}ms, border-color 0.3s, box-shadow 0.3s`,
                            }}
                        >
                            <div className="w-10 h-10 rounded-[10px] bg-[#FAF8F5] flex items-center justify-center text-xl mb-4 group-hover:scale-110 transition-transform duration-300">
                                {f.icon}
                            </div>
                            <h3 className="text-[15px] font-semibold text-[#2D2926] mb-2">{f.title}</h3>
                            <p className="text-[13px] text-[#9A8A7C] leading-[1.6]">{f.desc}</p>
                        </div>
                    ))}
                </div>
            </section>

            {/* ── CTA FINAL ── */}
            <section className="py-24 px-6">
                <div className="max-w-[700px] mx-auto text-center relative">
                    <div className="absolute inset-0 bg-gradient-to-br from-[#C97B4B]/8 to-[#9A8A7C]/8 rounded-[28px] blur-2xl" />
                    <div className="relative bg-white border border-[#EDE9E3] rounded-[24px] p-12 shadow-[0_8px_40px_rgba(45,41,38,0.06)]">
                        <div className="text-[11px] tracking-[2px] text-[#B8A99A] font-medium mb-4">ÎNCEPE AZI</div>
                        <h2 className="text-[34px] font-semibold tracking-[-1.5px] text-[#2D2926] leading-tight mb-4">
                            Gata să pui ordine<br />în finanțele familiei?
                        </h2>
                        <p className="text-[15px] text-[#9A8A7C] mb-8 leading-[1.6]">
                            Creează-ți contul gratuit și începe să gestionezi cheltuielile familiei tale în câteva minute.
                        </p>
                        <div className="flex items-center justify-center gap-4">
                            <Link
                                to="/register"
                                className="group px-8 py-4 bg-[#2D2926] text-white rounded-[12px] text-[15px] font-medium hover:bg-[#C97B4B] transition-all duration-300 shadow-md hover:shadow-lg hover:-translate-y-0.5 flex items-center gap-2"
                            >
                                Creează cont gratuit
                                <span className="group-hover:translate-x-1 transition-transform">→</span>
                            </Link>
                            <Link
                                to="/login"
                                className="text-[14px] font-medium text-[#9A8A7C] hover:text-[#2D2926] transition-colors"
                            >
                                Sau intră în cont
                            </Link>
                        </div>
                    </div>
                </div>
            </section>

            {/* ── FOOTER ── */}
            <footer className="border-t border-[#EDE9E3] py-8 px-6">
                <div className="max-w-[1100px] mx-auto flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-[6px] bg-[#2D2926] flex items-center justify-center text-white font-bold text-xs">
                            FA
                        </div>
                        <span className="text-[13px] font-medium text-[#9A8A7C]">FamilyAgent</span>
                    </div>
                    <p className="text-[12px] text-[#B8A99A]">© 2025 FamilyAgent · Versiune demo</p>
                </div>
            </footer>
        </div>
    );
}
