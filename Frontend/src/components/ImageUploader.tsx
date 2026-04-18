import React, { useState, useRef } from 'react';
import { UploadCloud, X, AlertCircle } from 'lucide-react';

interface ImageUploaderProps {
    onImageSelect: (file: File | null) => void;
}

export const ImageUploader: React.FC<ImageUploaderProps> = ({ onImageSelect }) => {
   
    const [preview, setPreview] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setError(null);
        const file = e.target.files?.[0];

        if (!file) {
            clearSelection();
            return;
        }

        // Validare criteriu 1: Tip fișier (doar imagini)
        if (!file.type.startsWith('image/')) {
            setError('Format invalid. Vă rugăm să selectați o imagine (.jpg, .png).');
            clearSelection();
            return;
        }

        // Validare criteriu 2: Dimensiune maximă (5MB)
        const MAX_SIZE = 5 * 1024 * 1024;
        if (file.size > MAX_SIZE) {
            setError('Fișierul depășește limita maximă de 5MB.');
            clearSelection();
            return;
        }

        // Generare preview imagine
        const objectUrl = URL.createObjectURL(file);
        setPreview(objectUrl);

        // Transmitere obiect File către componenta părinte
        onImageSelect(file);
    };

    const clearSelection = () => {
        setPreview(null);
        onImageSelect(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    return (
        <div className="flex flex-col w-full">

            {!preview ? (
                // Starea: Fără imagine (Upload Area)
                <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-[#EDE9E3] rounded-[10px] bg-[#FAF8F5] hover:bg-white hover:border-[#C4B9AC] cursor-pointer transition-all group">
                    <UploadCloud className="text-[#B8A99A] group-hover:text-[#C97B4B] transition-colors mb-2" size={28} />
                    <span className="text-[13px] font-medium text-[#2D2926] tracking-tight mb-1">
                        Apasă pentru a încărca
                    </span>
                    <span className="text-[11px] text-[#9A8A7C]">
                        PNG sau JPG
                    </span>
                    <input
                        type="file"
                        accept="image/*"
                        capture="environment"
                        onChange={handleFileChange}
                        ref={fileInputRef}
                        className="hidden"
                    />
                </label>
            ) : (
                // Starea: Cu imagine (Preview Area)
                <div className="relative w-full rounded-[10px] border border-[#EDE9E3] bg-white p-1.5 shadow-sm inline-block fade-in-up">
                    <img
                        src={preview}
                        alt="Preview bon fiscal"
                        className="w-full max-h-[200px] object-cover rounded-[6px]"
                    />
                    <button
                        type="button"
                        onClick={clearSelection}
                        className="absolute top-3 right-3 bg-white text-red-500 w-8 h-8 rounded-full flex items-center justify-center shadow-[0_2px_8px_rgba(0,0,0,0.15)] hover:bg-red-50 transition-colors"
                        title="Șterge imaginea"
                    >
                        <X size={16} strokeWidth={2.5} />
                    </button>
                </div>
            )}

            {/* Mesaj de Eroare */}
            {error && (
                <div className="mt-3 text-red-500 text-[12px] font-medium flex items-center gap-1.5 fade-in-up">
                    <AlertCircle size={14} />
                    {error}
                </div>
            )}

        </div>
    );
};