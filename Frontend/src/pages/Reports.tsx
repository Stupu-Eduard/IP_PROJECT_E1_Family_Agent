import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, RefreshCw, X, AlertCircle } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { api } from '../services/api';

interface ExpenseReportDTO {
    day: string;
    amount: number;
}

interface ApiExpense {
    id: number;
    amount: string | number;
    expenseDate: string | null;
}

// ─── Helper: calculează from/to din timeRange ─────────────────────────────────
function getRangeDates(range: string): { from: string; to: string } {
    const to  = new Date();
    const from = new Date();

    if (range === '1W') from.setDate(to.getDate() - 7);
    else if (range === '1M') from.setMonth(to.getMonth() - 1);
    else if (range === '3M') from.setMonth(to.getMonth() - 3);
    else if (range === '1Y') from.setFullYear(to.getFullYear() - 1);

    return {
        from: from.toISOString().slice(0, 10),
        to:   to.toISOString().slice(0, 10),
    };
}

// ─── Helper: grupează cheltuielile pe zi ──────────────────────────────────────
function groupByDay(expenses: ApiExpense[], from: string, to: string): ExpenseReportDTO[] {
    const fromDate = new Date(from);
    const toDate   = new Date(to);

    // Construim un map zi -> total
    const map: Record<string, number> = {};
    expenses.forEach((e) => {
        if (!e.expenseDate) return;
        const dateStr = e.expenseDate.slice(0, 10);
        const d = new Date(dateStr);
        if (d >= fromDate && d <= toDate) {
            map[dateStr] = (map[dateStr] ?? 0) + Number(e.amount);
        }
    });

    // Dacă intervalul e mare (>60 zile), grupăm pe săptămâni/luni pentru lizibilitate
    const diffDays = Math.round((toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays <= 31) {
        // Zi cu zi
        return Object.entries(map)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([dateStr, amount]) => ({
                day: new Date(dateStr).toLocaleDateString('ro-RO', { day: '2-digit', month: 'short' }),
                amount: Math.round(amount * 100) / 100,
            }));
    } else {
        // Grupăm pe săptămâni
        const weekMap: Record<string, number> = {};
        Object.entries(map).forEach(([dateStr, amount]) => {
            const d = new Date(dateStr);
            // Luni din săptămâna curentă
            const day   = d.getDay();
            const diff  = d.getDate() - day + (day === 0 ? -6 : 1);
            const monday = new Date(d.setDate(diff));
            const weekKey = monday.toISOString().slice(0, 10);
            weekMap[weekKey] = (weekMap[weekKey] ?? 0) + amount;
        });

        return Object.entries(weekMap)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([dateStr, amount]) => ({
                day: new Date(dateStr).toLocaleDateString('ro-RO', { day: '2-digit', month: 'short' }),
                amount: Math.round(amount * 100) / 100,
            }));
    }
}

// ─── Helper: parsare dată dd/mm/yyyy ─────────────────────────────────────────
const dateRegex = /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[012])\/\d{4}$/;
function parseDate(dateStr: string): Date | null {
    if (!dateRegex.test(dateStr)) return null;
    const [day, month, year] = dateStr.split('/');
    const parsed = new Date(`${year}-${month}-${day}T00:00:00`);
    if (
        parsed.getFullYear() === Number(year) &&
        parsed.getMonth() + 1 === Number(month) &&
        parsed.getDate() === Number(day)
    ) return parsed;
    return null;
}

export default function Reports() {
    const navigate = useNavigate();

    // ── State ──────────────────────────────────────────────────────────────
    const [timeRange,      setTimeRange]      = useState('1M');
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [isLoading,      setIsLoading]      = useState(false);
    const [loadError,      setLoadError]      = useState<string | null>(null);
    const [startDate,      setStartDate]      = useState('');
    const [endDate,        setEndDate]        = useState('');
    const [data,           setData]           = useState<ExpenseReportDTO[]>([]);
    const [totalAmount,    setTotalAmount]    = useState(0);

    // ── Validare date custom (NEATINSĂ) ────────────────────────────────────
    const startObj          = parseDate(startDate);
    const endObj            = parseDate(endDate);
    const isStartFormatError = startDate.length > 0 && !startObj;
    const isEndFormatError   = endDate.length > 0 && !endObj;
    const hasFormatError     = isStartFormatError || isEndFormatError;
    const isChronologyError  = Boolean(startObj && endObj && startObj > endObj);
    const isApplyDisabled    = !startObj || !endObj || hasFormatError || isChronologyError;

    // ── Fetch date din API ────────────────────────────────────────────────
    const fetchData = useCallback(async (from: string, to: string, signal?: AbortSignal) => {
        setIsLoading(true);
        setLoadError(null);
        try {
            const { data: expenses } = await api.get<ApiExpense[]>('/api/v1/expenses', { signal });
            const grouped = groupByDay(expenses, from, to);
            setData(grouped);
            setTotalAmount(
                Math.round(grouped.reduce((sum, item) => sum + item.amount, 0) * 100) / 100
            );
        } catch (err: any) {
            if (err?.name !== 'CanceledError' && err?.name !== 'AbortError') {
                setLoadError('Nu am putut încărca datele. Încearcă din nou.');
            }
        } finally {
            setIsLoading(false);
        }
    }, []);

    // ── Refetch la schimbarea timeRange (nu la CUSTOM) ────────────────────
    useEffect(() => {
        if (timeRange === 'CUSTOM') return;
        const controller = new AbortController();
        const { from, to } = getRangeDates(timeRange);
        void fetchData(from, to, controller.signal);
        return () => controller.abort();
    }, [timeRange, fetchData]);

    // ── Handlers (NEATINSE) ────────────────────────────────────────────────
    const handleApplyCustomDate = () => {
        if (isApplyDisabled || !startObj || !endObj) return;
        const from = startObj.toISOString().slice(0, 10);
        const to   = endObj.toISOString().slice(0, 10);
        setTimeRange('CUSTOM');
        void fetchData(from, to);
    };

    const handleCloseCustomDate = () => {
        setShowDatePicker(false);
        setStartDate('');
        setEndDate('');
    };

    return (
        <div style={{ maxWidth: 960, margin: '0 auto', width: '100%' }}>

            {/* ── Header ──────────────────────────────────────────────────── */}
            <div className="fade-up" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
                <button className="btn btn-ghost btn-icon" onClick={() => navigate('/dashboard')}>
                    <ArrowLeft size={16} />
                </button>
                <div className="chip chip-live">RAPOARTE · EVOLUȚIE</div>
            </div>
            <h1 className="h1 fade-up" style={{ marginBottom: 8 }}>Evoluție cheltuieli</h1>
            <div className="fade-up" style={{ color: 'var(--color-muted)', fontSize: 14, marginBottom: 16, lineHeight: 1.6 }}>
                Analizează cheltuielile familiei tale pe perioade personalizate.
            </div>

            {/* ── Eroare API ────────────────────────────────────────────────── */}
            {loadError && (
                <div className="fade-up" style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    background: '#FEF2F2', border: '1px solid #FECACA',
                    borderRadius: 12, padding: '12px 16px', marginBottom: 12,
                    color: '#DC2626', fontSize: 13, fontWeight: 500,
                }}>
                    <AlertCircle size={15} /> {loadError}
                </div>
            )}

            {/* ── Filtre + Total ───────────────────────────────────────────── */}
            <div className="card fade-up" style={{ padding: '12px 16px', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>

                    {/* Toggle preset (NEATINS) */}
                    <div className="tabs">
                        {['1W', '1M', '3M', '1Y'].map((range) => (
                            <button
                                key={range}
                                onClick={() => { setTimeRange(range); handleCloseCustomDate(); }}
                                className={`tab ${timeRange === range && !showDatePicker ? 'active' : ''}`}
                            >
                                {range}
                            </button>
                        ))}
                    </div>

                    {/* Buton interval custom (NEATINS) */}
                    <button
                        onClick={() => setShowDatePicker(!showDatePicker)}
                        className="btn btn-ghost"
                        style={{
                            fontSize: 13,
                            ...(showDatePicker || timeRange === 'CUSTOM'
                                ? { background: 'var(--color-primary-tint)', color: 'var(--color-primary)', borderColor: 'var(--color-primary-edge)' }
                                : {}),
                        }}
                    >
                        <Calendar size={14} /> Interval Custom
                    </button>

                    {/* Total perioadă — acum din date reale */}
                    <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 1, height: 28, background: 'var(--color-border)' }} />
                        <div>
                            <div className="label" style={{ marginBottom: 2 }}>TOTAL PERIOADĂ</div>
                            <div style={{ display: 'flex', alignItems: 'baseline', gap: 5 }}>
                                <span className="shimmer-num" style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.8px' }}>
                                    {isLoading ? '...' : totalAmount.toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </span>
                                <span style={{ fontSize: 12, color: 'var(--color-muted-2)', fontWeight: 500 }}>RON</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Panou date custom cu validare (NEATINS) */}
                {showDatePicker && (
                    <div className="fade-up" style={{ marginTop: 16, padding: 16, background: 'var(--color-bg)', borderRadius: 12, border: '1px solid var(--color-border)' }}>
                        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', gap: 10 }}>
                            <div>
                                <div className="label" style={{ marginBottom: 6 }}>De la (dd/mm/yyyy)</div>
                                <input
                                    type="text"
                                    placeholder="ex: 12/04/2026"
                                    maxLength={10}
                                    className="input"
                                    style={{
                                        width: 150, fontSize: 13,
                                        ...(isStartFormatError || isChronologyError
                                            ? { borderColor: '#FCA5A5', background: '#FEF2F2' } : {}),
                                    }}
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                />
                            </div>
                            <div>
                                <div className="label" style={{ marginBottom: 6 }}>Până la (dd/mm/yyyy)</div>
                                <input
                                    type="text"
                                    placeholder="ex: 20/04/2026"
                                    maxLength={10}
                                    className="input"
                                    style={{
                                        width: 150, fontSize: 13,
                                        ...(isEndFormatError || isChronologyError
                                            ? { borderColor: '#FCA5A5', background: '#FEF2F2' } : {}),
                                    }}
                                    value={endDate}
                                    onChange={(e) => setEndDate(e.target.value)}
                                />
                            </div>
                            <div style={{ display: 'flex', gap: 6 }}>
                                <button
                                    onClick={handleApplyCustomDate}
                                    disabled={isApplyDisabled}
                                    className="btn btn-primary"
                                    style={{ fontSize: 13, padding: '9px 16px', opacity: isApplyDisabled ? 0.35 : 1, cursor: isApplyDisabled ? 'not-allowed' : 'pointer' }}
                                >
                                    Aplică
                                </button>
                                <button onClick={handleCloseCustomDate} className="btn btn-ghost btn-icon">
                                    <X size={15} />
                                </button>
                            </div>
                        </div>

                        {/* Erori validare (NEATINSE) */}
                        {hasFormatError && (
                            <div className="fade-up" style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#DC2626', fontWeight: 500 }}>
                                <AlertCircle size={13} /> Te rugăm să introduci datele conform formatului (dd/mm/yyyy).
                            </div>
                        )}
                        {isChronologyError && !hasFormatError && (
                            <div className="fade-up" style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#DC2626', fontWeight: 500 }}>
                                <AlertCircle size={13} /> Data de început trebuie să fie înainte de data de sfârșit.
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* ── Grafic Area ──────────────────────────────────────────────── */}
            <div className="card fade-up" style={{ padding: '16px 20px', minHeight: 340, position: 'relative' }}>

                {/* Loading overlay */}
                {isLoading && (
                    <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.75)', zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(3px)', borderRadius: 20 }}>
                        <RefreshCw size={28} style={{ color: 'var(--color-primary)', animation: 'ring-rotate 0.9s linear infinite' }} />
                    </div>
                )}

                <div className="label" style={{ marginBottom: 14 }}>EVOLUȚIE CHELTUIELI</div>

                {!isLoading && data.length === 0 ? (
                    <div style={{ height: 300, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-muted)', fontSize: 14 }}>
                        Nu există cheltuieli în perioada selectată.
                    </div>
                ) : (
                    <div style={{ width: '100%', height: 300 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%"  stopColor="#C97B4B" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#C97B4B" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
                                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} dy={10} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} />
                                <Tooltip
                                    contentStyle={{ borderRadius: 12, border: '1px solid #EDE9E3', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', backgroundColor: '#fff' }}
                                    itemStyle={{ color: '#C97B4B', fontWeight: 500, fontSize: 14 }}
                                    labelStyle={{ color: '#9A8A7C', fontSize: 12, marginBottom: 4 }}
                                    cursor={{ stroke: '#EDE9E3', strokeWidth: 1, strokeDasharray: '4 4' }}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="amount"
                                    stroke="#C97B4B"
                                    strokeWidth={3}
                                    fillOpacity={1}
                                    fill="url(#colorAmount)"
                                    activeDot={{ r: 6, fill: '#C97B4B', stroke: '#fff', strokeWidth: 2 }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                )}
            </div>
        </div>
    );
}