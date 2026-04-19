import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, X, MessageSquare } from 'lucide-react';

const ChatAI: React.FC = () => {

    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        { id: 1, text: "Salut! Sunt asistentul tău FamilyAgent. Cum te pot ajuta cu bugetul astăzi?", sender: 'bot' }
    ]);
    const [input, setInput] = useState('');
    const [isTyping, setIsTyping] = useState(false); // State pentru "typing..." și dezactivare input

    // Referință pentru auto-scroll
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Efect pentru a face scroll automat la ultimul mesaj
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        if (isOpen) {
            scrollToBottom();
        }
    }, [messages, isTyping, isOpen]);

    const handleSend = (e: React.FormEvent) => {
        e.preventDefault();

        // Previne trimiterea dacă inputul e gol sau asistentul deja "scrie"
        if (!input.trim() || isTyping) return;

        const userMsg = { id: Date.now(), text: input, sender: 'user' };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setIsTyping(true); // Dezactivăm inputul și arătăm animația

        // Simulare request către backend
        setTimeout(() => {
            const botMsg = { id: Date.now() + 1, text: "Analizez datele tale financiare... (Simulare)", sender: 'bot' };
            setMessages(prev => [...prev, botMsg]);
            setIsTyping(false); // Reactivăm inputul
        }, 1500);
    };

    return (
        <div className="fixed bottom-6 right-6 md:bottom-8 md:right-8 z-50 flex flex-col items-end">

            {/* Butonul de activare (FAB) */}
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    className="w-[52px] h-[52px] bg-[#2D2926] text-white rounded-[14px] shadow-[0_8px_24px_rgba(45,41,38,0.25)] hover:scale-105 transition-transform flex items-center justify-center border-none cursor-pointer"
                >
                    <MessageSquare size={24} />
                </button>
            )}

            {/* Fereastra de Chat (ChatView) */}
            {isOpen && (
                <div className="bg-white border border-[#EDE9E3] shadow-[0_12px_40px_rgba(0,0,0,0.12)] w-[320px] md:w-[380px] h-[500px] flex flex-col rounded-[20px] overflow-hidden fade-in-up">

                    {/* Header Chat */}
                    <div className="bg-[#2D2926] px-5 py-4 text-[#FAF8F5] flex justify-between items-center shrink-0">
                        <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-[8px] bg-white/10 flex items-center justify-center">
                                <Bot size={18} className="text-white" />
                            </div>
                            <div>
                                <h3 className="font-medium text-[14px] leading-tight tracking-tight">FamilyAgent AI</h3>
                                <p className="text-[11px] text-white/60">Asistent Virtual</p>
                            </div>
                        </div>
                        <button
                            onClick={() => setIsOpen(false)}
                            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-white/10 text-white/80 hover:text-white transition-colors"
                        >
                            <X size={18} />
                        </button>
                    </div>

                    {/* Zona scrollabilă de mesaje */}
                    <div className="flex-1 overflow-y-auto p-5 space-y-4 bg-[#FAF8F5]">
                        {messages.map((msg) => (
                            <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                                <div
                                    className={`max-w-[85%] p-3.5 text-[13px] leading-relaxed shadow-sm ${
                                        msg.sender === 'user'
                                            ? 'bg-[#2D2926] text-white rounded-[14px] rounded-br-sm' // UserBubble (dreapta)
                                            : 'bg-white text-[#2D2926] border border-[#EDE9E3] rounded-[14px] rounded-bl-sm' // AgentBubble (stânga)
                                    }`}
                                >
                                    {msg.text}
                                </div>
                            </div>
                        ))}

                        {/* Indicator "Typing..." */}
                        {isTyping && (
                            <div className="flex justify-start fade-in-up">
                                <div className="bg-white border border-[#EDE9E3] p-3.5 rounded-[14px] rounded-bl-sm shadow-sm flex gap-1.5 items-center h-[42px]">
                                    <span className="w-1.5 h-1.5 bg-[#B8A99A] rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                                    <span className="w-1.5 h-1.5 bg-[#B8A99A] rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                                    <span className="w-1.5 h-1.5 bg-[#B8A99A] rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                                </div>
                            </div>
                        )}

                        {/* Element invizibil pentru auto-scroll */}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Sticky input area la bază */}
                    <form onSubmit={handleSend} className="p-4 bg-white border-t border-[#EDE9E3] flex gap-2 shrink-0">
                        <input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            disabled={isTyping} // Dezactivare input
                            placeholder={isTyping ? "Agentul scrie..." : "Întreabă ceva..."}
                            className="flex-1 bg-[#FAF8F5] border border-[#EDE9E3] rounded-[12px] px-4 py-2.5 text-[13px] text-[#2D2926] placeholder:text-[#C4B9AC] focus:outline-none focus:border-[#C4B9AC] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                        />
                        <button
                            type="submit"
                            disabled={!input.trim() || isTyping} // Dezactivare buton trimitere
                            className="bg-[#2D2926] text-white w-[42px] h-[42px] rounded-[12px] hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center shrink-0"
                        >
                            <Send size={16} className="ml-1" />
                        </button>
                    </form>
                </div>
            )}
        </div>
    );
};

export default ChatAI;