import { ref } from 'vue'
import { errorMessage } from '../services/productService'

export function useConfirmation() {
  const confirmation = ref(null), confirming = ref(false), confirmError = ref('')
  let operation = null
  function askConfirmation(prompt, action) {
    if (confirmation.value || confirming.value) return false
    confirmError.value = ''
    operation = action
    confirmation.value = prompt
    return true
  }
  function cancelConfirmation() {
    if (confirming.value) return
    confirmation.value = null
    confirmError.value = ''
    operation = null
  }
  async function confirmAction() {
    if (confirming.value || !operation) return
    confirming.value = true
    confirmError.value = ''
    try {
      await operation()
      confirmation.value = null
      operation = null
    } catch (error) { confirmError.value = errorMessage(error) }
    finally { confirming.value = false }
  }
  return { confirmation, confirming, confirmError, askConfirmation, cancelConfirmation, confirmAction }
}

export function statusConfirmation(kind, name, status) {
  const stopping = status === 1
  return {
    title: `${stopping ? 'Ngừng hoạt động' : 'Kích hoạt'} ${kind}?`,
    message: `Bạn có chắc muốn ${stopping ? 'ngừng hoạt động' : 'kích hoạt lại'} ${name}?`,
    details: [{ label: 'Trạng thái sau khi xác nhận', value: stopping ? 'Ngừng hoạt động' : 'Hoạt động' }]
  }
}
