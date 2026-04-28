import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, RefreshCw, X, AlertCircle } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

interface ExpenseReportDTO {
    day: string;
    amount: number;
}

export default function Reports() {
    const navigate = useNavigate();

    const [timeRange, setTimeRange] = useState('1M');
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const [data] = useState<ExpenseReportDTO[]>([
        { day: '01 Apr', amount: 120 }, { day: '05 Apr', amount: 450 },
        { day: '10 Apr', amount: 300 }, { day: '15 Apr', amount: 800 },
        { day: '20 Apr', amount: 200 }, { day: '25 Apr', amount: 550 },
        { day: '30 Apr', amount: 400 },
    ]);

    const totalAmount = data.reduce((sum, item) => sum + item.amount, 0);

    const dateRegex = /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[012])\/\d{4}$/;

    const parseDate = (dateStr: string): Date | null => {
        if (!dateRegex.test(dateStr)) return null;
        const [day, month, year] = dateStr.split('/');
        const parsedDate = new Date(`${year}-${month}-${day}T00:00:00`);
        if (parsedDate.getFullYear() === Number(year) &&
            parsedDate.getMonth() + 1 === Number(month) &&
            parsedDate.getDate() === Number(day)) {
            return parsedDate;
        }
        return null;
    };

    const startObj = parseDate(startDate);
    const endObj = parseDate(endDate);

    const isStartFormatError = startDate.length > 0 && !startObj;
    const isEndFormatError = endDate.length > 0 && !endObj;
    const hasFormatError = isStartFormatError || isEndFormatError;
    const isChronologyError = Boolean(startObj && endObj && startObj > endObj);
    const isApplyDisabled = !startObj || !endObj || hasFormatError || isChronologyError;

    useEffect(() => {
        setIsLoading(true);
        const timer = setTimeout(() => setIsLoading(false), 800);
        return () => clearTimeout(timer);
    }, [timeRange]);

    const handleApplyCustomDate = () => {
        if (!isApplyDisabled) {
            setTimeRange('CUSTOM');
            setIsLoading(true);
            setTimeout(() => setIsLoading(false), 800);
        }
    };

    const handleCloseCustomDate = () => {
        setShowDatePicker(false);
        setStartDate('');
        setEndDate('');
    };


    const inputStyle = "bg-white border border-[#EDE9E3] rounded-[10px] px-3.5 py-2.5 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors w-[140px]";
    const inputErrorStyle = "!border-red-300 !bg-red-50/50 focus:!border-red-400";

    return (
        <div className="px-6 lg:px-10 pt-10 pb-20 max-w-[960px] mx-auto w-full flex-1">

                {/* Header Pagina */}
                <div className="flex items-center gap-4 mb-8 fade-in-up">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="w-10 h-10 bg-white border border-[#EDE9E3] rounded-[10px] flex items-center justify-center text-[#2D2926] hover:border-[#C4B9AC] transition-colors shadow-sm shrink-0"
                    >
                        <ArrowLeft size={18} />
                    </button>
                    <div>
                        <h2 className="text-[24px] font-medium text-[#2D2926] tracking-tight leading-tight">Evoluție Cheltuieli</h2>
                    </div>
                </div>

                {/* Zona de Filtrare & KPI */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-8 fade-in-up" style={{ animationDelay: '0.1s' }}>

                    {/* Controale Grafic */}
                    <div className="lg:col-span-2 bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-[0_4px_20px_rgba(0,0,0,0.02)] flex flex-col justify-center">
                        <div className="flex flex-wrap items-center justify-between gap-4">

                            {/* Toggle Butoane Pre-setate */}
                            <div className="flex gap-1.5 bg-[#F4F0EB] p-1.5 rounded-[12px]">
                                {['1W', '1M', '3M', '1Y'].map((range) => (
                                    <button
                                        key={range}
                                        onClick={() => { setTimeRange(range); handleCloseCustomDate(); }}
                                        className={`px-4 py-2 rounded-[8px] text-[13px] font-medium transition-all ${
                                            timeRange === range && !showDatePicker
                                                ? 'bg-[#2D2926] text-white shadow-sm'
                                                : 'text-[#8C7E6E] hover:text-[#2D2926]'
                                        }`}
                                    >
                                        {range}
                                    </button>
                                ))}
                            </div>

                            {/* Buton Custom */}
                            <button
                                onClick={() => setShowDatePicker(!showDatePicker)}
                                className={`flex items-center gap-2 text-[13px] font-medium transition-colors px-4 py-2.5 rounded-[10px] border ${
                                    showDatePicker || timeRange === 'CUSTOM'
                                        ? 'bg-[#FFF8F2] text-[#C97B4B] border-[#F0DFD0]'
                                        : 'bg-white text-[#2D2926] border-[#EDE9E3] hover:border-[#C4B9AC]'
                                }`}
                            >
                                <Calendar size={16} />
                                <span>Interval Custom</span>
                            </button>
                        </div>

                        {/* Panoul pentru Interval Custom cu Validare */}
                        {showDatePicker && (
                            <div className="mt-5 p-5 bg-[#FAF8F5] rounded-[12px] border border-[#EDE9E3] fade-in-up">
                                <div className="flex flex-wrap items-end gap-3">
                                    <div>
                                        <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">De la (dd/mm/yyyy)</label>
                                        <input
                                            type="text"
                                            placeholder="ex: 12/04/2026"
                                            maxLength={10}
                                            className={`${inputStyle} ${isStartFormatError || isChronologyError ? inputErrorStyle : ''}`}
                                            value={startDate}
                                            onChange={(e) => setStartDate(e.target.value)}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">Până la (dd/mm/yyyy)</label>
                                        <input
                                            type="text"
                                            placeholder="ex: 20/04/2026"
                                            maxLength={10}
                                            className={`${inputStyle} ${isEndFormatError || isChronologyError ? inputErrorStyle : ''}`}
                                            value={endDate}
                                            onChange={(e) => setEndDate(e.target.value)}
                                        />
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            onClick={handleApplyCustomDate}
                                            disabled={isApplyDisabled}
                                            className="px-5 py-2.5 bg-[#2D2926] text-white rounded-[10px] text-[13px] font-medium hover:opacity-90 disabled:opacity-30 disabled:cursor-not-allowed transition-opacity h-[39px] flex items-center justify-center"
                                        >
                                            Aplică
                                        </button>
                                        <button
                                            onClick={handleCloseCustomDate}
                                            className="w-[39px] h-[39px] bg-white border border-[#EDE9E3] text-[#2D2926] rounded-[10px] flex items-center justify-center hover:border-[#C4B9AC] transition-colors"
                                            title="Anulează"
                                        >
                                            <X size={16} />
                                        </button>
                                    </div>
                                </div>

                                {/* Mesaje de Eroare */}
                                {hasFormatError && (
                                    <div className="mt-4 text-red-500 text-[12px] font-medium flex items-center gap-1.5 fade-in-up">
                                        <AlertCircle size={14} />
                                        Te rugăm să introduci datele conform formatului (dd/mm/yyyy).
                                    </div>
                                )}
                                {isChronologyError && !hasFormatError && (
                                    <div className="mt-4 text-red-500 text-[12px] font-medium flex items-center gap-1.5 fade-in-up">
                                        <AlertCircle size={14} />
                                        Eroare: Data de început trebuie să fie înainte de data de sfârșit.
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Card Total Perioadă */}
                    <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-[0_4px_20px_rgba(0,0,0,0.02)] flex flex-col justify-center relative overflow-hidden">
                        <div className="absolute left-0 top-0 bottom-0 w-[4px] bg-[#C97B4B] rounded-l-[14px]"></div>
                        <p className="text-[11px] tracking-[1px] text-[#B8A99A] font-medium mb-2 uppercase">Total Perioadă</p>
                        <h3 className="text-[32px] font-medium text-[#2D2926] tracking-[-1px] leading-none">
                            {totalAmount.toFixed(2)} <span className="text-[14px] font-normal text-[#B8A99A] tracking-normal">RON</span>
                        </h3>
                    </div>
                </div>

                {/* Grafic Area */}
                <div className="bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-[0_4px_20px_rgba(0,0,0,0.02)] fade-in-up relative" style={{ minHeight: '400px', animationDelay: '0.2s' }}>

                    {/* Loading Overlay */}
                    {isLoading && (
                        <div className="absolute inset-0 bg-white/70 z-10 flex items-center justify-center backdrop-blur-sm rounded-[14px]">
                            <RefreshCw className="animate-spin text-[#C97B4B]" size={32} />
                        </div>
                    )}

                    <div style={{ width: '100%', height: 350 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#C97B4B" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="#C97B4B" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#EDE9E3" />
                                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} dy={10} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#B8A99A' }} />
                                <Tooltip
                                    contentStyle={{ borderRadius: '12px', border: '1px solid #EDE9E3', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', backgroundColor: '#fff' }}
                                    itemStyle={{ color: '#C97B4B', fontWeight: 500, fontSize: '14px' }}
                                    labelStyle={{ color: '#9A8A7C', fontSize: '12px', marginBottom: '4px' }}
                                    cursor={{ stroke: '#EDE9E3', strokeWidth: 1, strokeDasharray: '4 4' }}
                                />
                                <Area type="monotone" dataKey="amount" stroke="#C97B4B" strokeWidth={3} fillOpacity={1} fill="url(#colorAmount)" activeDot={{ r: 6, fill: '#C97B4B', stroke: '#fff', strokeWidth: 2 }} />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
        </div>
    );
}