import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, X, MessageSquare } from 'lucide-react';
import api from '../api/api';

const ChatAI: React.FC = () => {

    // ── State ────────────────────────────────────────────────────────────
    const [isOpen,   setIsOpen]   = useState(false);
    const [messages, setMessages] = useState([
        { id: 1, text: 'Salut! Sunt asistentul tău FamilyAgent. Cum te pot ajuta cu bugetul astăzi?', sender: 'bot' }
    ]);
    const [input,    setInput]    = useState('');
    const [isTyping, setIsTyping] = useState(false);

    // ── Auto-scroll ───────────────────────────────────────────────────────
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const scrollToBottom = () => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); };
    useEffect(() => { if (isOpen) scrollToBottom(); }, [messages, isTyping, isOpen]);

    // ── handleSend ────────────────────────────────────────────────────────
    const handleSend = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!input.trim() || isTyping) return;

        const userMsg = { id: Date.now(), text: input, sender: 'user' };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setIsTyping(true);

        try {
            const { data } = await api.post('/v1/chat', { message: input });
            setMessages(prev => [...prev, {
                id: Date.now() + 1,
                text: data.reply,
                sender: 'bot',
            }]);
        } catch (err) {
            setMessages(prev => [...prev, {
                id: Date.now() + 1,
                text: 'Eroare la conectarea cu asistentul. Încearcă din nou.',
                sender: 'bot',
            }]);
        } finally {
            setIsTyping(false);
        }
    };

    return (
        <>
            {/* ── FAB button ─────────────────────────────────────────────── */}
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    style={{
                        position: 'fixed',
                        bottom: 24,
                        right: 24,
                        zIndex: 50,
                        width: 44,
                        height: 44,
                        borderRadius: 13,
                        background: 'var(--color-ink)',
                        border: 'none',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        boxShadow: '0 4px 16px rgba(45,41,38,0.20)',
                        transition: 'transform 0.2s var(--ease-spring), box-shadow 0.2s ease, background 0.2s ease',
                    }}
                    onMouseEnter={e => {
                        const el = e.currentTarget as HTMLElement;
                        el.style.transform = 'scale(1.08)';
                        el.style.boxShadow = '0 8px 24px rgba(45,41,38,0.25)';
                        el.style.background = 'var(--color-primary)';
                    }}
                    onMouseLeave={e => {
                        const el = e.currentTarget as HTMLElement;
                        el.style.transform = 'scale(1)';
                        el.style.boxShadow = '0 4px 16px rgba(45,41,38,0.20)';
                        el.style.background = 'var(--color-ink)';
                    }}
                    aria-label="Deschide asistentul AI"
                >
                    <MessageSquare size={18} color="#fff" />
                </button>
            )}

            {/* ── Chat window ────────────────────────────────────────────── */}
            {isOpen && (
                <div
                    className="fade-up"
                    style={{
                        position: 'fixed',
                        bottom: 24,
                        right: 24,
                        zIndex: 50,
                        width: 300,
                        height: 400,
                        display: 'flex',
                        flexDirection: 'column',
                        background: '#fff',
                        border: '1px solid var(--color-border)',
                        borderRadius: 16,
                        overflow: 'hidden',
                        boxShadow: '0 8px 32px rgba(45,41,38,0.12), 0 2px 8px rgba(45,41,38,0.06)',
                    }}
                >
                    {/* Header */}
                    <div style={{
                        background: 'var(--color-ink)',
                        padding: '10px 14px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        flexShrink: 0,
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{
                                width: 26, height: 26, borderRadius: 7,
                                background: 'rgba(255,255,255,0.1)',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                            }}>
                                <Bot size={14} color="#fff" />
                            </div>
                            <div>
                                <div style={{ fontSize: 12.5, fontWeight: 500, color: '#fff' }}>
                                    FamilyAgent AI
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 1 }}>
                                    <span style={{ width: 4, height: 4, borderRadius: '50%', background: '#4CAF7D', display: 'inline-block' }} />
                                    <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)' }}>Online</span>
                                </div>
                            </div>
                        </div>
                        <button
                            onClick={() => setIsOpen(false)}
                            style={{
                                width: 26, height: 26, borderRadius: 7,
                                background: 'rgba(255,255,255,0.08)',
                                border: 'none', cursor: 'pointer',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                color: 'rgba(255,255,255,0.6)',
                                transition: 'background 0.15s ease',
                            }}
                            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.16)')}
                            onMouseLeave={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.08)')}
                        >
                            <X size={13} />
                        </button>
                    </div>

                    {/* Messages */}
                    <div style={{
                        flex: 1,
                        overflowY: 'auto',
                        padding: '12px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 8,
                        background: 'var(--color-bg)',
                        scrollbarWidth: 'thin',
                        scrollbarColor: '#EDE9E3 transparent',
                    }}>
                        {messages.map((msg) => (
                            <div key={msg.id} style={{
                                display: 'flex',
                                justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                            }}>
                                <div style={{
                                    maxWidth: '85%',
                                    padding: '8px 11px',
                                    fontSize: 12.5,
                                    lineHeight: 1.5,
                                    borderRadius: msg.sender === 'user'
                                        ? '12px 12px 3px 12px'
                                        : '12px 12px 12px 3px',
                                    background: msg.sender === 'user' ? 'var(--color-ink)' : '#fff',
                                    color: msg.sender === 'user' ? '#fff' : 'var(--color-ink)',
                                    border: msg.sender === 'user' ? 'none' : '1px solid var(--color-border)',
                                    boxShadow: '0 1px 3px rgba(45,41,38,0.05)',
                                }}>
                                    {msg.text}
                                </div>
                            </div>
                        ))}

                        {/* Typing indicator */}
                        {isTyping && (
                            <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                                <div style={{
                                    background: '#fff',
                                    border: '1px solid var(--color-border)',
                                    borderRadius: '12px 12px 12px 3px',
                                    padding: '8px 12px',
                                    display: 'flex',
                                    gap: 4,
                                    alignItems: 'center',
                                }}>
                                    {[0, 150, 300].map((delay) => (
                                        <span key={delay} style={{
                                            width: 5, height: 5,
                                            background: 'var(--color-muted-2)',
                                            borderRadius: '50%',
                                            display: 'inline-block',
                                            animation: 'bounce-dot 1.2s ease-in-out infinite',
                                            animationDelay: `${delay}ms`,
                                        }} />
                                    ))}
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input */}
                    <form
                        onSubmit={handleSend}
                        style={{
                            padding: '10px',
                            background: '#fff',
                            borderTop: '1px solid var(--color-border)',
                            display: 'flex',
                            gap: 6,
                            flexShrink: 0,
                        }}
                    >
                        <input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            disabled={isTyping}
                            placeholder={isTyping ? 'Agentul scrie...' : 'Întreabă ceva...'}
                            className="input"
                            style={{
                                flex: 1,
                                fontSize: 12.5,
                                padding: '7px 11px',
                                background: 'var(--color-bg)',
                                opacity: isTyping ? 0.6 : 1,
                                borderRadius: 9,
                            }}
                        />
                        <button
                            type="submit"
                            disabled={!input.trim() || isTyping}
                            style={{
                                width: 34, height: 34,
                                background: 'var(--color-ink)',
                                border: 'none',
                                borderRadius: 9,
                                color: '#fff',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexShrink: 0,
                                opacity: (!input.trim() || isTyping) ? 0.4 : 1,
                                transition: 'opacity 0.2s ease, background 0.2s ease',
                            }}
                            onMouseEnter={e => { if (input.trim() && !isTyping) (e.currentTarget as HTMLElement).style.background = 'var(--color-primary)'; }}
                            onMouseLeave={e => (e.currentTarget as HTMLElement).style.background = 'var(--color-ink)'}
                        >
                            <Send size={13} style={{ marginLeft: 1 }} />
                        </button>
                    </form>
                </div>
            )}
        </>
    );
};

export default ChatAI;