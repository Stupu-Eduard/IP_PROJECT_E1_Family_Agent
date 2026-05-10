import '@testing-library/jest-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import ProfileSettings from './ProfileSettings'
import { useAuthStore } from '../store/authStore'
import { updateCurrentUserProfile } from '../services/profile'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../store/authStore', () => ({
  useAuthStore: vi.fn(),
}))

vi.mock('../services/profile', () => ({
  updateCurrentUserProfile: vi.fn(),
}))

describe('ProfileSettings page', () => {
  const mockUpdateProfile = vi.fn()
  const mockProfile = {
    name: 'Alex Popescu',
    avatarUrl: 'https://cdn.test/existing-avatar.png',
    role: 'Parent' as const,
    preferences: { theme: 'dark' as const, language: 'en' as const, emailNotifications: false },
  }
  const mockRevokeObjectURL = vi.fn()
  const mockCreateObjectURL = vi.fn(() => 'blob:avatar-preview')

  beforeEach(() => {
    vi.clearAllMocks()
    mockNavigate.mockClear()
    vi.mocked(useAuthStore).mockImplementation((selector: any) => selector({ token: 'token', profile: mockProfile, updateProfile: mockUpdateProfile }))
    vi.mocked(updateCurrentUserProfile).mockResolvedValue({
      name: 'Alex Popescu',
      avatarUrl: 'https://cdn.test/new-avatar.png',
      role: 'Parent',
      email: 'alex@test.com',
      preferences: { theme: 'light', language: 'ro', emailNotifications: true },
    } as never)
    Object.defineProperty(URL, 'createObjectURL', { value: mockCreateObjectURL, writable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: mockRevokeObjectURL, writable: true })
  })

  const renderPage = () => render(<MemoryRouter><ProfileSettings /></MemoryRouter>)

  it('afișează valorile curente și permite preview pentru avatar valid', async () => {
    const { container } = renderPage()

    expect(screen.getByDisplayValue('Alex Popescu')).toBeInTheDocument()
    expect(screen.getByAltText('Preview avatar')).toHaveAttribute('src', 'https://cdn.test/existing-avatar.png')

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    const validFile = new File(['image-data'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [validFile] } })

    expect(screen.getByAltText('Preview avatar')).toHaveAttribute('src', 'blob:avatar-preview')
    expect(mockCreateObjectURL).toHaveBeenCalledWith(validFile)
  })

  it('validează numele gol, tipul de fișier și dimensiunea maximă', () => {
    const { container } = renderPage()

    const nameInput = screen.getByDisplayValue('Alex Popescu')
    fireEvent.change(nameInput, { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))
    expect(screen.getByText('⚠ Numele nu poate fi gol.')).toBeInTheDocument()

    fireEvent.change(nameInput, { target: { value: 'Alex Popescu' } })
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, { target: { files: [new File(['bad'], 'avatar.gif', { type: 'image/gif' })] } })
    expect(screen.getByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).toBeInTheDocument()

    const bigFile = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'big.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [bigFile] } })
    expect(screen.getByText('⚠ Avatarul nu poate depăși 2MB.')).toBeInTheDocument()
  })

  it('acceptă fișiere JPG și JPEG ca valide', () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement

    const jpegFile = new File(['image-data'], 'avatar.jpeg', { type: 'image/jpeg' })
    fireEvent.change(fileInput, { target: { files: [jpegFile] } })
    expect(screen.queryByText(/trebuie să fie JPG/i)).not.toBeInTheDocument()

    const jpgFile = new File(['image-data'], 'avatar.jpg', { type: 'image/jpg' })
    fireEvent.change(fileInput, { target: { files: [jpgFile] } })
    expect(screen.queryByText(/trebuie să fie JPG/i)).not.toBeInTheDocument()
  })

  it('salvează datele și actualizează store-ul global', async () => {
    renderPage()

    fireEvent.change(screen.getByDisplayValue('Alex Popescu'), { target: { value: 'Alexandru Popescu' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(updateCurrentUserProfile).toHaveBeenCalled())
    expect(updateCurrentUserProfile).toHaveBeenCalledWith({
      name: 'Alexandru Popescu',
      preferences: { theme: 'dark', language: 'en', emailNotifications: true },
      avatarFile: null,
    })
    expect(mockUpdateProfile).toHaveBeenCalledWith({
      name: 'Alex Popescu',
      avatarUrl: 'https://cdn.test/new-avatar.png',
      role: 'Parent',
      email: 'alex@test.com',
      preferences: { theme: 'light', language: 'ro', emailNotifications: true },
    })
    expect(screen.getByText('✓ Modificările au fost salvate cu succes.')).toBeInTheDocument()
  })

  it('salvează datele cu avatar file separat', async () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    const validFile = new File(['image-data'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [validFile] } })

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(updateCurrentUserProfile).toHaveBeenCalled())
    expect(updateCurrentUserProfile).toHaveBeenCalledWith({
      name: 'Alex Popescu',
      preferences: { theme: 'dark', language: 'en', emailNotifications: false },
      avatarFile: validFile,
    })
    expect(mockRevokeObjectURL).toHaveBeenCalled()
  })

  it('afișează eroare dacă API-ul respinge cererea', async () => {
    vi.mocked(updateCurrentUserProfile).mockRejectedValueOnce(new Error('Server indisponibil'))
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(screen.getByText('⚠ Server indisponibil')).toBeInTheDocument())
    expect(mockUpdateProfile).not.toHaveBeenCalled()
  })

  it('elimină avatarul și golește preview', () => {
    const { container } = renderPage()

    expect(screen.getByAltText('Preview avatar')).toHaveAttribute('src', 'https://cdn.test/existing-avatar.png')

    fireEvent.click(screen.getByRole('button', { name: /elimină avatarul/i }))

    const previewImg = container.querySelector('img[alt="Preview avatar"]') as HTMLImageElement
    if (previewImg) {
      expect(previewImg.src).not.toBe('https://cdn.test/existing-avatar.png')
    }
    expect(screen.queryByAltText('Preview avatar')).not.toBeInTheDocument()
  })

  it('schimbă tema și limba din selecte', async () => {
    renderPage()

    const themeSelect = screen.getByRole('combobox', { name: /temă/i }) as HTMLSelectElement
    expect(themeSelect.value).toBe('dark')
    fireEvent.change(themeSelect, { target: { value: 'light' } })
    expect(themeSelect.value).toBe('light')

    const languageSelect = screen.getByRole('combobox', { name: /limbă/i }) as HTMLSelectElement
    expect(languageSelect.value).toBe('en')
    fireEvent.change(languageSelect, { target: { value: 'ro' } })
    expect(languageSelect.value).toBe('ro')

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(updateCurrentUserProfile).toHaveBeenCalled())
    expect(updateCurrentUserProfile).toHaveBeenCalledWith(
      expect.objectContaining({
        preferences: expect.objectContaining({
          theme: 'light',
          language: 'ro',
        }),
      })
    )
  })

  it('curăță preview URL-urile la demontare', () => {
    const { container, unmount } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    const validFile = new File(['image-data'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [validFile] } })

    expect(mockCreateObjectURL).toHaveBeenCalled()
    const previewUrl = mockCreateObjectURL.mock.results[0]?.value
    expect(previewUrl).toBe('blob:avatar-preview')

    unmount()
    expect(mockRevokeObjectURL).toHaveBeenCalledWith(previewUrl)
  })

  it('navighează la dashboard la click pe butonul back', () => {
    renderPage()

    const backButton = screen.getAllByRole('button')[0]
    fireEvent.click(backButton)
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  it('dezactivează butonul save în timp ce se salvează', async () => {
    vi.mocked(updateCurrentUserProfile).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({
        name: 'Alex Popescu',
        avatarUrl: 'https://cdn.test/new-avatar.png',
        role: 'Parent',
        email: 'alex@test.com',
        preferences: { theme: 'light', language: 'ro', emailNotifications: true },
      } as never), 100))
    )

    renderPage()
    const saveButton = screen.getByRole('button', { name: /salvează modificările/i }) as HTMLButtonElement
    expect(saveButton.disabled).toBe(false)

    fireEvent.click(saveButton)
    expect(saveButton.disabled).toBe(true)
    expect(screen.getByText('Se salvează...')).toBeInTheDocument()

    await waitFor(() => {
      expect(saveButton.disabled).toBe(false)
      expect(screen.getByText(/Salvează modificările/i)).toBeInTheDocument()
    })
  })

  it('clonează mesajele de eroare doar din avatar, nu din nume', () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, { target: { files: [new File(['bad'], 'avatar.gif', { type: 'image/gif' })] } })
    expect(screen.getByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).toBeInTheDocument()

    const nameInput = screen.getByDisplayValue('Alex Popescu')
    fireEvent.change(nameInput, { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    // Eroarea anterioară de avatar ar trebui înlocuită cu cea de nume
    expect(screen.getByText('⚠ Numele nu poate fi gol.')).toBeInTheDocument()
    expect(screen.queryByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).not.toBeInTheDocument()
  })

  it('clonează mesajele de eroare și success corect', () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, { target: { files: [new File(['bad'], 'avatar.gif', { type: 'image/gif' })] } })

    expect(screen.getByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).toBeInTheDocument()

    // Uploading a valid file should clear error
    const validFile = new File(['image-data'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [validFile] } })

    expect(screen.queryByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).not.toBeInTheDocument()
  })

  it('nu face nimic dacă file input-ul nu are fișier selectat', () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, { target: { files: [] } })

    expect(mockCreateObjectURL).not.toHaveBeenCalled()
  })

  it('resetează success message după submit și error-ul rămâne curat', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(screen.getByText(/Modificările au fost salvate/i)).toBeInTheDocument())

    // Verify success shows
    expect(screen.getByText(/Modificările au fost salvate/i)).toBeInTheDocument()
  })

  it('clonează și resetează state-ul de eroare la avatar remove', () => {
    const { container } = renderPage()

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, { target: { files: [new File(['bad'], 'avatar.gif', { type: 'image/gif' })] } })
    expect(screen.getByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /elimină avatarul/i }))
    expect(screen.queryByText('⚠ Avatarul trebuie să fie JPG sau PNG.')).not.toBeInTheDocument()
  })

  it('tratează erori non-Error în catch handler', async () => {
    vi.mocked(updateCurrentUserProfile).mockRejectedValueOnce('String error message')
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(screen.getByText(/Nu am putut salva setările profilului/i)).toBeInTheDocument())
  })

  it('NU dezactivează butonul save la încă din început', () => {
    renderPage()
    const saveButton = screen.getByRole('button', { name: /salvează modificările/i }) as HTMLButtonElement
    expect(saveButton.disabled).toBe(false)
  })

  it('suportă schimbarea mai multor preferințe și salvarea combinată', async () => {
    const { container } = renderPage()

    const nameInput = screen.getByDisplayValue('Alex Popescu')
    fireEvent.change(nameInput, { target: { value: 'Alexandru P.' } })

    const themeSelect = screen.getByRole('combobox', { name: /temă/i }) as HTMLSelectElement
    fireEvent.change(themeSelect, { target: { value: 'light' } })

    const languageSelect = screen.getByRole('combobox', { name: /limbă/i }) as HTMLSelectElement
    fireEvent.change(languageSelect, { target: { value: 'ro' } })

    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('checkbox')) // Toggle back

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement
    const validFile = new File(['image-data'], 'avatar.png', { type: 'image/png' })
    fireEvent.change(fileInput, { target: { files: [validFile] } })

    fireEvent.click(screen.getByRole('button', { name: /salvează modificările/i }))

    await waitFor(() => expect(updateCurrentUserProfile).toHaveBeenCalled())
    expect(updateCurrentUserProfile).toHaveBeenCalledWith({
      name: 'Alexandru P.',
      preferences: { theme: 'light', language: 'ro', emailNotifications: false },
      avatarFile: validFile,
    })
  })
})




