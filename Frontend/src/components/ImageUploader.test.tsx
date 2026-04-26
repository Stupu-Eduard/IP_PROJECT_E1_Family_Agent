import '@testing-library/jest-dom';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ImageUploader } from './ImageUploader';

// Mock pentru URL.createObjectURL (necesar deoarece funcția nu există în mediul izolat de test Node/JSDOM)
beforeEach(() => {
    global.URL.createObjectURL = vi.fn(() => 'mock-preview-url');
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('ImageUploader Component', () => {
    const mockOnImageSelect = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('1. Randează starea inițială corect', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);
        expect(screen.getByText('Apasă pentru a încărca')).toBeInTheDocument();
        expect(screen.getByText('PNG sau JPG')).toBeInTheDocument();
    });

    it('2. Afișează eroare dacă formatul fișierului este invalid', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);

        // Găsim input-ul ascuns folosind label-ul său
        const input = screen.getByLabelText(/Apasă pentru a încărca/i);

        // Simulăm încărcarea unui fișier text
        const file = new File(['text content'], 'document.txt', { type: 'text/plain' });
        fireEvent.change(input, { target: { files: [file] } });

        expect(screen.getByText('Format invalid. Vă rugăm să selectați o imagine (.jpg, .png).')).toBeInTheDocument();
        expect(mockOnImageSelect).toHaveBeenCalledWith(null);
    });

    it('3. Afișează eroare dacă fișierul depășește limita de 5MB', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);
        const input = screen.getByLabelText(/Apasă pentru a încărca/i);

        // Simulăm un fișier imagine de 6MB
        const file = new File([''], 'huge_image.jpg', { type: 'image/jpeg' });
        Object.defineProperty(file, 'size', { value: 6 * 1024 * 1024 });

        fireEvent.change(input, { target: { files: [file] } });

        expect(screen.getByText('Fișierul depășește limita maximă de 5MB.')).toBeInTheDocument();
        expect(mockOnImageSelect).toHaveBeenCalledWith(null);
    });

    it('4. Procesează cu succes o imagine validă și afișează preview-ul', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);
        const input = screen.getByLabelText(/Apasă pentru a încărca/i);

        // Simulăm o imagine validă
        const file = new File(['dummy content'], 'test.png', { type: 'image/png' });
        fireEvent.change(input, { target: { files: [file] } });

        // Verificăm apariția imaginii de preview
        const previewImage = screen.getByAltText('Preview bon fiscal');
        expect(previewImage).toBeInTheDocument();
        expect(previewImage).toHaveAttribute('src', 'mock-preview-url');

        // Verificăm dacă fișierul a fost transmis corect către componenta părinte
        expect(mockOnImageSelect).toHaveBeenCalledWith(file);

        // Verificăm că erorile nu sunt prezente
        expect(screen.queryByText(/Format invalid/)).not.toBeInTheDocument();
    });

    it('5. Resetează selecția la apăsarea butonului de ștergere (X)', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);
        const input = screen.getByLabelText(/Apasă pentru a încărca/i);

        // Încărcăm imaginea
        const file = new File(['dummy content'], 'test.png', { type: 'image/png' });
        fireEvent.change(input, { target: { files: [file] } });

        // Identificăm butonul X și dăm click pe el
        const deleteButton = screen.getByTitle('Șterge imaginea');
        fireEvent.click(deleteButton);

        // Verificăm că preview-ul a dispărut și componenta părinte a primit "null"
        expect(screen.queryByAltText('Preview bon fiscal')).not.toBeInTheDocument();
        expect(mockOnImageSelect).toHaveBeenCalledWith(null);
        expect(screen.getByText('Apasă pentru a încărca')).toBeInTheDocument();
    });

    it('6. Tratează corect cazul în care selecția fișierului este anulată de utilizator', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />);
        const input = screen.getByLabelText(/Apasă pentru a încărca/i);

        // Simulăm un change event cu un array gol de fișiere
        fireEvent.change(input, { target: { files: [] } });

        expect(mockOnImageSelect).toHaveBeenCalledWith(null);
        expect(screen.getByText('Apasă pentru a încărca')).toBeInTheDocument();
    });
});