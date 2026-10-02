// Dữ liệu mẫu cho nhóm Sản phẩm (thay bằng API khi có backend)
const GRAY = { name: 'Xám', code: 'MS05', hex: '#8a94a6' }
const BLACK = { name: 'Đen', code: 'MS01', hex: '#0f172a' }
const WHITE = { name: 'Trắng', code: 'WH01', hex: '#ffffff' }
const RED = { name: 'Đỏ', code: 'RD01', hex: '#dc2626' }
const BLUE = { name: 'Xanh', code: 'BL01', hex: '#2563eb' }

const mk = (pc, color, size, price, qty) => ({
  pc, code: `${pc}-${color.code}-${size}`, color: color.name, hex: color.hex, size, qty, price, discount: 20, status: 'Đang bán'
})

export const brands = ['New Balance', 'Nike', 'Brooks', 'Hoka', 'Salomon', 'ASICS', 'Puma']
export const shoeTypes = ['Sneakers', 'Walking', 'Running', 'Hiking']
export const colorOptions = [GRAY, BLACK, WHITE, RED, BLUE]
export const sizeOptions = ['36', '37', '38', '39', '40', '41', '42', '43']

export const products = [
  { code: 'G57664', name: 'New Balance 530', brand: 'New Balance', type: 'Sneakers', price: 2000000, old: 2500000, status: 'Kinh doanh',
    variants: [mk('G57664', WHITE, '40', 2500000, 300), mk('G57664', GRAY, '41', 2500000, 288)] },
  { code: 'G86428', name: 'Air Jordan 1 Low G', brand: 'Nike', type: 'Walking', price: 2000000, old: 2500000, status: 'Kinh doanh',
    variants: [mk('G86428', RED, '41', 2500000, 200), mk('G86428', BLACK, '42', 2500000, 194)] },
  { code: 'SP20', name: 'Brooks Ghost 14', brand: 'Brooks', type: 'Running', price: 3080000, old: 3700000, status: 'Kinh doanh',
    variants: [mk('SP20', GRAY, '41', 3700000, 150), mk('SP20', BLACK, '42', 3700000, 150)] },
  { code: 'SP19', name: 'Hoka Clifton 8', brand: 'Hoka', type: 'Running', price: 2960000, old: 3700000, status: 'Kinh doanh',
    variants: [mk('SP19', BLUE, '40', 3700000, 150), mk('SP19', GRAY, '41', 3700000, 150)] },
  { code: 'SP18', name: 'Salomon Speedcross 5', brand: 'Salomon', type: 'Hiking', price: 2840000, old: 3700000, status: 'Kinh doanh',
    variants: [mk('SP18', BLACK, '40', 3700000, 150), mk('SP18', RED, '41', 3700000, 150)] },
  { code: 'SP17', name: 'ASICS GEL-Kayano 31', brand: 'ASICS', type: 'Running', price: 3032000, old: 3790000, status: 'Kinh doanh',
    variants: [mk('G66748', GRAY, '37', 3790000, 100), mk('G66748', GRAY, '36', 3790000, 100), mk('G66748', BLACK, '37', 3790000, 100), mk('G66748', BLACK, '36', 3790000, 100)] }
]
