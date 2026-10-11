export function confirmAction(message, onConfirm, options = {}) {
  window.dispatchEvent(new CustomEvent('ss:confirm', {
    detail: {
      message,
      title: options.title || 'Xác nhận',
      confirmText: options.confirmText || 'Đồng ý',
      cancelText: options.cancelText || 'Hủy',
      onConfirm
    }
  }))
}

export function showSuccess(message, title = 'Thành công!') {
  window.dispatchEvent(new CustomEvent('ss:toast', {
    detail: { message, title, type: 'success' }
  }))
}

export function showError(message, title = 'Thất bại!') {
  window.dispatchEvent(new CustomEvent('ss:toast', {
    detail: { message, title, type: 'error' }
  }))
}

export function askToLeave(message) {
  return new Promise(resolve => {
    window.dispatchEvent(new CustomEvent('ss:confirm', {
      detail: { message, title: 'Rời khỏi trang?', confirmText: 'Rời trang', cancelText: 'Ở lại',
        onConfirm: () => resolve(true), onCancel: () => resolve(false) }
    }))
  })
}
