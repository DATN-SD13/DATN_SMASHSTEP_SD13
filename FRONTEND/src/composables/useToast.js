import { ref } from 'vue'

export function useToast() {
  const toast = ref({
    show: false,
    type: 'success',
    title: '',
    message: ''
  })

  let timer = null

  function showToast(
    message,
    type = 'success',
    title = null
  ) {
    if (timer) {
      clearTimeout(timer)
    }

    toast.value = {
      show: true,
      type,

      title:
        title ||
        (
          type === 'success'
            ? 'Thành công'
            : type === 'error'
              ? 'Có lỗi xảy ra'
              : 'Thông báo'
        ),

      message
    }

    timer = setTimeout(() => {
      toast.value.show = false
    }, 3000)
  }

  function success(message, title = 'Thành công') {
    showToast(
      message,
      'success',
      title
    )
  }

  function error(message, title = 'Có lỗi xảy ra') {
    showToast(
      message,
      'error',
      title
    )
  }

  function closeToast() {
    toast.value.show = false

    if (timer) {
      clearTimeout(timer)
    }
  }

  return {
    toast,
    showToast,
    success,
    error,
    closeToast
  }
}