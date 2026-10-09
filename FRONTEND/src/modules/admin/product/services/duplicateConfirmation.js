import { productSummary } from './productService'

export function productDuplicatePrompt(existing, race = false) {
  return {
    title: 'Sản phẩm đã tồn tại',
    message: (race ? 'Sản phẩm này vừa được tạo bởi thao tác khác.\n' : '')
      + 'Đã có sản phẩm có cùng tên và toàn bộ thuộc tính. Bạn có muốn cập nhật sản phẩm hiện có không?',
    confirmText: 'Cập nhật sản phẩm',
    details: [{ label: 'ID', value: existing.id }, ...productSummary(existing)]
  }
}

export function attributeDuplicatePrompt(label, existing, reason) {
  const lower = label.toLowerCase()
  return {
    title: `${label} đã tồn tại`,
    message: `Đã có ${lower} tương tự trong hệ thống. Bạn có muốn cập nhật ${lower} hiện có không?`,
    confirmText: `Cập nhật ${lower}`,
    details: [{ label: 'ID', value: existing.id },
      ...(existing.ma ? [{ label: 'Mã', value: existing.ma }] : []),
      { label: 'Tên / giá trị', value: existing.ten },
      ...(existing.maMauHex ? [{ label: 'Mã HEX', value: existing.maMauHex }] : []),
      { label: 'Trạng thái', value: existing.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' },
      { label: 'Trùng', value: reason === 'HEX' ? 'Mã HEX' : reason === 'NAME_AND_HEX' ? 'Tên và mã HEX' : 'Tên / giá trị' }]
  }
}
