// Dữ liệu địa chỉ mẫu (thay bằng API tỉnh/thành khi có backend)
export const provinces = {
  'Hà Nội': ['Phường Cầu Giấy', 'Phường Thanh Xuân', 'Phường Hoàn Kiếm', 'Phường Hai Bà Trưng', 'Phường Đống Đa'],
  'TP. Hồ Chí Minh': ['Phường Bến Nghé', 'Phường Bến Thành', 'Phường Thảo Điền', 'Phường Tân Định'],
  'Đà Nẵng': ['Phường Hải Châu', 'Phường Thanh Khê', 'Phường Sơn Trà'],
  'Tỉnh Phú Thọ': ['Phường Vĩnh Yên', 'Phường Gia Cẩm', 'Phường Tiên Cát']
}
export const provinceNames = Object.keys(provinces)
export const wardsOf = (p) => provinces[p] || []
