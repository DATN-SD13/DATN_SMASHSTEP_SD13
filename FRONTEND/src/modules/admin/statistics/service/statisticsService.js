
import api from '../../../../utils/api'

// API lấy dữ liệu 2 biểu đồ
export const getStatisticsChart = (params) => {
  return api.get('/thong-ke-tong-hop/bieu-do', { params })
}

// API lấy dữ liệu 7 ô tổng quan doanh thu
export const getStatisticsSummary = (params) => {
  return api.get('/thong-ke-tong-hop/tong-quan', { params })
}


// API thống kê doanh thu theo chu kỳ thời gian
export const getStatisticsPeriod = () => {
  return api.get('/thong-ke-tong-hop/chu-ky')
}


// API thống kê trạng thái đơn hàng
export const getStatisticsOrderStatus = (params) => {
  return api.get('/thong-ke', { params })
}


// API thống kê doanh thu theo nhân viên
export const getStatisticsEmployee = (params) => {
  return api.get('/thong-ke-tong-hop/nhan-vien', { params })
}


// API thống kê tổng quan sản phẩm
export const getStatisticsProduct = (params) => {
  return api.get('/thong-ke-tong-hop/san-pham', { params })
}

// Reuse actual product attribute data for the dashboard brand filter.
export async function getStatisticsBrands() {
  const brands = []
  let page = 0, totalPages = 1
  do {
    const response = await api.get('/product-attributes/brands', { params: { page, size: 100, status: 1 } })
    const data = response.data
    brands.push(...(data.content || []).map(brand => ({ id: brand.id, name: brand.ten })))
    totalPages = data.totalPages || 1
    page++
  } while (page < totalPages)
  return brands
}

/**
 * API so sánh doanh thu theo tháng, quý hoặc năm
 * nhomTheo: month | quarter | year
 * nam: năm cần thống kê
 */
export const getStatisticsRevenueComparison = (params) => {
  return api.get(
    '/thong-ke-tong-hop/so-sanh-doanh-thu',
    { params }
  );
};
