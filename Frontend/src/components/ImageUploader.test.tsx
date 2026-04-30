import '@testing-library/jest-dom'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ImageUploader } from './ImageUploader'

describe('ImageUploader Component', () => {
    const mockOnImageSelect = vi.fn()
    let inputClickMock: any

    beforeEach(() => {
        vi.clearAllMocks()
        global.URL.createObjectURL = vi.fn(() => 'mock-url')
        inputClickMock = vi.fn()
        window.HTMLInputElement.prototype.click = inputClickMock
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    const createFile = (name: string, size: number, type: string) => {
        const file = new File([''], name, { type })
        Object.defineProperty(file, 'size', { value: size })
        return file
    }

    it('1. Randează starea inițială de dropzone', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        expect(screen.getByText('Trage bonul aici')).toBeInTheDocument()
    })

    it('2. Arată eroare la selectare manuală: fișierul nu este imagine', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const input = container.querySelector('input[type="file"]') as HTMLInputElement
        const pdfFile = createFile('test.pdf', 1024, 'application/pdf')

        fireEvent.change(input, { target: { files: [pdfFile] } })

        expect(screen.getByText(/Format invalid/i)).toBeInTheDocument()
        expect(mockOnImageSelect).toHaveBeenCalledWith(null)
    })

    it('3. Arată eroare la selectare manuală: fișier prea mare', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const input = container.querySelector('input[type="file"]') as HTMLInputElement
        const bigFile = createFile('big.jpg', 6 * 1024 * 1024, 'image/jpeg')

        fireEvent.change(input, { target: { files: [bigFile] } })

        expect(screen.getByText(/depășește limita maximă/i)).toBeInTheDocument()
    })

    it('4. Resetează corect dacă utilizatorul dă cancel la dialog (empty files)', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const input = container.querySelector('input[type="file"]') as HTMLInputElement

        fireEvent.change(input, { target: { files: [] } })

        expect(mockOnImageSelect).toHaveBeenCalledWith(null)
    })

    it('5. Încarcă cu succes o imagine și afișează preview-ul', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const input = container.querySelector('input[type="file"]') as HTMLInputElement
        const validFile = createFile('bon.jpg', 1024, 'image/jpeg')

        fireEvent.change(input, { target: { files: [validFile] } })

        expect(global.URL.createObjectURL).toHaveBeenCalledWith(validFile)
        expect(screen.getByText('Bon încărcat')).toBeInTheDocument()
        expect(mockOnImageSelect).toHaveBeenCalledWith(validFile)
    })

    it('6. Șterge imaginea selectată', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const input = container.querySelector('input[type="file"]') as HTMLInputElement
        const validFile = createFile('bon.jpg', 1024, 'image/jpeg')

        fireEvent.change(input, { target: { files: [validFile] } })

        const deleteBtn = screen.getByTitle('Șterge imaginea')
        fireEvent.click(deleteBtn)

        expect(screen.getByText('Trage bonul aici')).toBeInTheDocument()
        expect(mockOnImageSelect).toHaveBeenCalledWith(null)
    })

    it('7. Gestionează evenimentele vizuale de Drag Over și Leave', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement

        fireEvent.dragOver(dropzone)
        expect(screen.getByText('Eliberează ca să încărcăm bonul')).toBeInTheDocument()

        fireEvent.dragLeave(dropzone)
        expect(screen.getByText('Trage bonul aici')).toBeInTheDocument()
    })

    it('8. Încarcă cu succes o imagine prin Drag & Drop', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement

        const validFile = createFile('drag.png', 1024, 'image/png')
        fireEvent.drop(dropzone, { dataTransfer: { files: [validFile] } })

        expect(mockOnImageSelect).toHaveBeenCalledWith(validFile)
    })

    it('9. Arată eroare la Drop: fișierul nu este imagine', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement
        const pdfFile = createFile('test.pdf', 1024, 'application/pdf')

        fireEvent.drop(dropzone, { dataTransfer: { files: [pdfFile] } })

        expect(screen.getByText(/Format invalid/i)).toBeInTheDocument()
    })

    it('10. Arată eroare la Drop: fișier prea mare', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement
        const bigFile = createFile('big.jpg', 6 * 1024 * 1024, 'image/jpeg')

        fireEvent.drop(dropzone, { dataTransfer: { files: [bigFile] } })

        expect(screen.getByText(/depășește limita maximă/i)).toBeInTheDocument()
    })

    it('11. Ignoră drop-ul dacă nu există fișiere în payload', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement

        fireEvent.drop(dropzone, { dataTransfer: { files: undefined } })
        expect(mockOnImageSelect).not.toHaveBeenCalled()
    })

    it('12. Permite accesibilitate prin tastatură (Enter pe dropzone)', () => {
        const { container } = render(<ImageUploader onImageSelect={mockOnImageSelect} />)
        const dropzone = container.querySelector('.dropzone') as HTMLElement

        fireEvent.keyDown(dropzone, { key: 'Space' })
        expect(inputClickMock).not.toHaveBeenCalled()

        fireEvent.keyDown(dropzone, { key: 'Enter' })
        expect(inputClickMock).toHaveBeenCalledTimes(1)
    })

    it('13. Declanșează input-ul la click pe butonul interior (Folosește camera)', () => {
        render(<ImageUploader onImageSelect={mockOnImageSelect} />)

        const cameraBtn = screen.getByText(/folosește camera/i)

        fireEvent.click(cameraBtn)

        expect(inputClickMock).toHaveBeenCalled()
    })
})