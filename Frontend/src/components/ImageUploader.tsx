import React, { useState, useRef } from 'react';

interface ImageUploaderProps {
    onImageSelect: (file: File | null) => void;
}

// ── Constante (NEATINSE) ──────────────────────────────────────────────────────
const MAX_SIZE = 5 * 1024 * 1024;

export const ImageUploader: React.FC<ImageUploaderProps> = ({ onImageSelect }) => {

    // ── State (NEATINS) ───────────────────────────────────────────────────────
    const [preview,  setPreview]  = useState<string | null>(null);
    const [error,    setError]    = useState<string | null>(null);
    const [isDrag,   setIsDrag]   = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // ── Handlers (LOGICĂ NEATINSĂ) ────────────────────────────────────────────
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setError(null);
        const file = e.target.files?.[0];
        if (!file) { clearSelection(); return; }
        if (!file.type.startsWith('image/')) {
            setError('Format invalid. Vă rugăm să selectați o imagine (.jpg, .png).');
            clearSelection(); return;
        }
        if (file.size > MAX_SIZE) {
            setError('Fișierul depășește limita maximă de 5MB.');
            clearSelection(); return;
        }
        const objectUrl = URL.createObjectURL(file);
        setPreview(objectUrl);
        onImageSelect(file);
    };

    const clearSelection = () => {
        setPreview(null);
        onImageSelect(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    // ── Drag handlers (noi, dar nu afectează logica existentă) ───────────────
    const onDragOver  = (e: React.DragEvent) => { e.preventDefault(); setIsDrag(true); };
    const onDragLeave = () => setIsDrag(false);
    const onDrop      = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDrag(false);
        const file = e.dataTransfer.files?.[0];
        if (!file) return;
        // Refolosim aceeași validare ca handleFileChange
        setError(null);
        if (!file.type.startsWith('image/')) {
            setError('Format invalid. Vă rugăm să selectați o imagine (.jpg, .png).');
            return;
        }
        if (file.size > MAX_SIZE) {
            setError('Fișierul depășește limita maximă de 5MB.');
            return;
        }
        const objectUrl = URL.createObjectURL(file);
        setPreview(objectUrl);
        onImageSelect(file);
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>

            {!preview ? (
                /* ── Dropzone alive ─────────────────────────────────────────────── */
                <div
                    className={`dropzone ${isDrag ? 'is-drag' : ''}`}
                    onDragOver={onDragOver}
                    onDragLeave={onDragLeave}
                    onDrop={onDrop}
                    onClick={() => fileInputRef.current?.click()}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
                    style={{ minHeight: 220 }}
                >
                    <input
                        type="file"
                        accept="image/*"
                        capture="environment"
                        onChange={handleFileChange}
                        ref={fileInputRef}
                        style={{ display: 'none' }}
                    />

                    {/* Scan line apare doar în is-drag */}
                    {isDrag && <div className="scan-line" />}

                    <span className="upload-icon">
            {isDrag ? (
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="9" cy="9" r="1.5"/>
                    <path d="m4 17 5-5 4 4 3-3 5 5"/>
                </svg>
            ) : (
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 16V4"/><path d="m7 9 5-5 5 5"/>
                    <path d="M5 16v3a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-3"/>
                </svg>
            )}
          </span>

                    <div style={{ fontSize: 18, fontWeight: 500, color: 'var(--color-ink)', marginTop: 18, letterSpacing: '-0.3px' }}>
                        {isDrag ? 'Eliberează ca să încărcăm bonul' : 'Trage bonul aici'}
                    </div>
                    <div style={{ fontSize: 13, color: 'var(--color-muted)', marginTop: 6 }}>
                        sau{' '}
                        <span style={{ color: 'var(--color-primary)', fontWeight: 500, textDecoration: 'underline', textUnderlineOffset: 3 }}>
              răsfoiește din galerie
            </span>
                        {' '}· JPG, PNG · max 5 MB
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 22 }}>
                        <button
                            className="btn btn-primary"
                            onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
                            style={{ fontSize: 13, padding: '9px 16px' }}
                        >
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M3 8h4l2-3h6l2 3h4v11H3z"/><circle cx="12" cy="13" r="3.6"/>
                            </svg>
                            Folosește camera
                        </button>
                    </div>
                </div>

            ) : (
                /* ── Preview imagine (STRUCTURA NEATINSĂ) ────────────────────── */
            <div style={{
                position: 'relative', width: '100%', borderRadius: 16,
                border: '1px solid var(--color-primary-edge)', background: 'var(--color-primary-tint)',
                padding: 8, overflow: 'hidden',
            }} className="fade-up">
                {/* Badge succes */}
                <div style={{
                    position: 'absolute', top: 14, left: 14, zIndex: 10,
                    display: 'flex', alignItems: 'center', gap: 6,
                    background: 'rgba(255,255,255,0.95)', borderRadius: 20, padding: '5px 10px',
                    fontSize: 11, fontWeight: 500, color: '#2E7B4F',
                    boxShadow: '0 2px 8px rgba(45,41,38,0.12)',
                }}>
            <span style={{ width: 14, height: 14, borderRadius: '50%', background: '#4CAF7D', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="m4 12 5 5 11-12"/></svg>
            </span>
                    Bon încărcat
                </div>

                <img
                    src={preview}
                    alt="Preview bon fiscal"
                    style={{ width: '100%', maxHeight: 240, objectFit: 'cover', borderRadius: 10, display: 'block' }}
                />

                {/* Buton ștergere (NEATINS — apelează clearSelection) */}
                <button
                    type="button"
                    onClick={clearSelection}
                    style={{
                        position: 'absolute', top: 12, right: 12, zIndex: 10,
                        background: 'white', color: '#DC2626',
                        width: 32, height: 32, borderRadius: '50%',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)', border: 'none', cursor: 'pointer',
                        transition: 'background 0.2s ease',
                    }}
                    title="Șterge imaginea"
                >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M6 6l12 12"/><path d="M18 6 6 18"/>
                    </svg>
                </button>
            </div>
            )}

            {/* ── Eroare validare (NEATINS) ──────────────────────────────────── */}
            {error && (
                <div style={{
                    marginTop: 10, display: 'flex', alignItems: 'center', gap: 8,
                    color: '#DC2626', fontSize: 12, fontWeight: 500,
                }} className="fade-up">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><circle cx="12" cy="16" r="1"/>
                    </svg>
                    {error}
                </div>
            )}
        </div>
    );
};