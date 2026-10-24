package com.smashstep.datn.invoice.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto;
import com.smashstep.datn.invoice.dto.HoaDonChiTietDto;
import com.smashstep.datn.invoice.dto.HoaDonDetailDto;
import com.smashstep.datn.invoice.dto.HoaDonListDto;
import com.smashstep.datn.invoice.dto.LichSuHoaDonDto;
import com.smashstep.datn.invoice.dto.LichSuThanhToanDto;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.enums.HinhThucNhan;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import com.smashstep.datn.invoice.repository.HoaDonChiTietRepository;
import com.smashstep.datn.invoice.repository.HoaDonRepository;
import com.smashstep.datn.invoice.repository.LichSuHoaDonRepository;
import com.smashstep.datn.invoice.repository.LichSuThanhToanRepository;
import com.smashstep.datn.invoice.rules.HoaDonRules;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final NhanVienRepository nhanVienRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired private InvoiceCodeService dichVuMaHoaDon;
    @org.springframework.beans.factory.annotation.Autowired private com.smashstep.datn.storefront.checkout.OrderEmailNotifications orderMail;

    @Transactional(readOnly = true)
    public PageResponse<HoaDonListDto> timKiem(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            String trangThai,
            String loaiDon,
            int page,
            int size) {
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            throw AppException.badRequest("Từ ngày không được sau đến ngày");
        }
        int pageNumber = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 100);
        if ((long) (pageNumber - 1) * (pageSize) > Integer.MAX_VALUE)
            throw AppException.badRequest("Trang yêu cầu vượt giới hạn phân trang");
        Integer status = getTrangThai(trangThai);
        Integer type = getLoaiDon(loaiDon);
        Pageable pageable =
                PageRequest.of(
                        pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "ngayTao", "id"));
        Specification<HoaDon> specification = taoBoLoc(ma, tuNgay, denNgay, status, type);
        Page<HoaDon> result = hoaDonRepository.findAll(specification, pageable);
        return PageResponse.from(result.map(HoaDonListDto::from));
    }

    @Transactional(readOnly = true)
    public HoaDonDetailDto chiTiet(String ma) {
        HoaDon hoaDon = timHoaDon(ma);
        List<HoaDonChiTietDto> items =
                hoaDonChiTietRepository.findByIdHoaDonIdOrderByIdAsc(hoaDon.getId()).stream()
                        .map(HoaDonChiTietDto::from)
                        .toList();
        List<LichSuThanhToanDto> paymentHistory =
                lichSuThanhToanRepository
                        .findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId())
                        .stream()
                        .map(LichSuThanhToanDto::from)
                        .toList();
        List<LichSuHoaDonDto> history = layLichSu(hoaDon.getId());
        var result = HoaDonDetailDto.from(hoaDon, items, paymentHistory, history);
        com.smashstep.datn.invoice.rules.PosPriceSnapshot.apply(
                result, lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(hoaDon.getId()));
        var failureRows = lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(hoaDon.getId());
        result.deliveryFailure(com.smashstep.datn.invoice.rules.DeliveryFailureRules.attempts(failureRows));
        if (Integer.valueOf(7).equals(hoaDon.getTrangThai())) {
            try { result.refundDue(com.smashstep.datn.invoice.rules.DeliveryFailureRules.refundDue(hoaDon,
                    lichSuThanhToanRepository.findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId()))); }
            catch (AppException e) { result.refundBlocked(e.getMessage()); }
        }
        result.returnedGoods(com.smashstep.datn.invoice.rules.ReturnedGoodsRules.handled(failureRows));
        return result;
    }

    @Transactional(readOnly = true)
    public List<LichSuHoaDonDto> lichSu(String ma) {
        HoaDon hoaDon = timHoaDon(ma);
        return layLichSu(hoaDon.getId());
    }

    @Transactional
    public HoaDonDetailDto capNhatTrangThai(String ma, CapNhatTrangThaiHoaDonDto request) {
        if (request != null
                && (com.smashstep.datn.invoice.rules.PosPriceSnapshot.metadata(request.getGhiChu())
                || com.smashstep.datn.invoice.rules.ReturnedGoodsRules.reserved(request.getGhiChu())
                || com.smashstep.datn.invoice.rules.PosHistoryRules.danhRieng(
                request.getGhiChu())))
            throw AppException.badRequest("Ghi chú chứa tiền tố dành riêng cho snapshot giá");
        if (request == null || request.getTrangThai() == null) {
            throw AppException.badRequest("Trạng thái không được để trống");
        }
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(request.getTrangThai());
        if (trangThai == null) {
            throw AppException.badRequest("Trạng thái hóa đơn không hợp lệ");
        }
        if (ma == null || ma.isBlank()) {
            throw AppException.badRequest("Mã hóa đơn không được để trống");
        }
        if (dichVuMaHoaDon != null) dichVuMaHoaDon.lockInvoiceCreation();
        HoaDon hoaDon =
                hoaDonRepository
                        .findByMaHoaDonForUpdate(ma.trim())
                        .orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn: " + ma));
        if (com.smashstep.datn.storefront.checkout.OnlineBankPayment.bankOrder(entityManager, hoaDon)
                && request.getTrangThai() != 6 && hoaDon.getNgayThanhToan() == null)
            throw AppException.conflict("Cần đối soát và xác nhận đã nhận đủ tiền QR trước khi xử lý đơn hàng");
        if (com.smashstep.datn.invoice.rules.PosDraftRules.managed(hoaDon)) {
            checkDraftOwner(hoaDon);
            if ("POS".equals(request.getNguonThaoTac())
                    && (hoaDon.getNgayThanhToan() != null || request.getTrangThai() != 6))
                throw AppException.badRequest(
                        "Nguồn POS chỉ dùng để hủy hóa đơn chờ chưa thanh toán");
            if (hoaDon.getNgayThanhToan() == null) {
                if (!Integer.valueOf(8).equals(hoaDon.getTrangThai())
                        || !lichSuThanhToanRepository
                        .findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId())
                        .isEmpty())
                    throw AppException.conflict("Hóa đơn không còn là đơn chờ chưa thanh toán");
                if (request.getTrangThai() != 6)
                    throw AppException.conflict("Hóa đơn chờ phải được thanh toán qua POS");
            } else if (!com.smashstep.datn.invoice.rules.PosDraftRules.delivery(hoaDon)
                    || request.getTrangThai() == 6
                    || request.getTrangThai() == 7) {
                throw AppException.conflict(
                        "Hóa đơn đã thanh toán không được sửa/hủy qua luồng này; cần nghiệp vụ hoàn"
                                + " tiền riêng");
            }
            request.setIdNhanVien(com.smashstep.datn.sales.service.PosActor.employeeId());
        }
        Integer trangThaiHienTai = hoaDon.getTrangThai();
        boolean failedTransition = Integer.valueOf(9).equals(trangThaiHienTai) || request.getTrangThai()==9;
        long failureCount = com.smashstep.datn.invoice.rules.DeliveryFailureRules.attempts(
                lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(hoaDon.getId()));
        if (failedTransition) {
            if (com.smashstep.datn.invoice.rules.ReturnedGoodsRules.handled(lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(hoaDon.getId()))
                    || lichSuThanhToanRepository.findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId()).stream().anyMatch(p -> Integer.valueOf(2).equals(p.getTrangThai())))
                throw AppException.conflict("Đơn đã xử lý hàng trả hoặc hoàn tiền; không thể giao lại");
            if (request.getSoLanThatBai()==null || request.getSoLanThatBai()!=failureCount)
                throw AppException.conflict("Số lần giao hàng đã thay đổi; tải lại hóa đơn");
            com.smashstep.datn.invoice.rules.DeliveryFailureRules.validate(
                    trangThaiHienTai, request.getTrangThai(), HoaDonRules.resolveHinhThucNhan(hoaDon)==HinhThucNhan.GIAO_HANG,
                    failureCount,request.getGhiChu());
            request.setIdNhanVien(com.smashstep.datn.sales.service.PosActor.employeeId());
            if(request.getTrangThai()==9) request.setGhiChu("Giao hàng thất bại lần " + (failureCount+1) + "/3: " + request.getGhiChu().trim());
        }
        if (java.util.Objects.equals(trangThaiHienTai, request.getTrangThai())) {
            throw AppException.badRequest("Trạng thái mới phải khác trạng thái hiện tại.");
        }

        if (failedTransition) {
            // Validated against the locked invoice and persisted failure history above.
        } else if (request.getTrangThai() == TrangThaiHoaDon.DA_HUY.getMa()) {
            if (Integer.valueOf(3).equals(trangThaiHienTai)
                    || hoaDon.getNgayThanhToan()!=null
                    || !lichSuThanhToanRepository.findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId()).isEmpty())
                throw AppException.conflict("Đơn đã giao cho vận chuyển hoặc có khoản thu: cần giao thất bại và xử lý nhận lại hàng/hoàn tiền trước khi hủy");
            if (trangThaiHienTai != null
                    && trangThaiHienTai == TrangThaiHoaDon.HOAN_THANH.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn thành, không thể hủy.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.DA_HUY.getMa()) {
                throw AppException.badRequest("Đơn hàng đã được hủy, không thể hủy lại.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_TIEN.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn tiền, không thể hủy.");
            }
            HinhThucNhan hinhThucNhan = HoaDonRules.resolveHinhThucNhan(hoaDon);
            // Hóa đơn chờ (8) là phiếu tạm giữ tại POS, cho phép hủy riêng để giải phóng phiếu.
            if (Integer.valueOf(TrangThaiHoaDon.HOA_DON_CHO.getMa()).equals(trangThaiHienTai)) {
                // continue
            } else {
                int trangThaiCuoiCoTheHuy =
                        hinhThucNhan == HinhThucNhan.NHAN_TAI_QUAY
                                ? TrangThaiHoaDon.DA_XAC_NHAN.getMa()
                                : TrangThaiHoaDon.DA_GIAO_HANG.getMa();
                if (trangThaiHienTai == null
                        || trangThaiHienTai < TrangThaiHoaDon.CHO_XAC_NHAN.getMa()
                        || trangThaiHienTai > trangThaiCuoiCoTheHuy) {
                    throw AppException.badRequest("Đơn hàng hiện tại không thể hủy.");
                }
            }
        } else {
            if (trangThaiHienTai != null
                    && trangThaiHienTai == TrangThaiHoaDon.HOAN_THANH.getMa()) {
                throw AppException.badRequest(
                        "Đơn hàng đã hoàn thành, không thể cập nhật trạng thái.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.DA_HUY.getMa()) {
                throw AppException.badRequest(
                        "Đơn hàng đã được hủy, không thể cập nhật trạng thái.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_TIEN.getMa()) {
                throw AppException.badRequest(
                        "Đơn hàng đã hoàn tiền, không thể cập nhật trạng thái.");
            }
            HinhThucNhan hinhThucNhan = HoaDonRules.resolveHinhThucNhan(hoaDon);
            TrangThaiHoaDon trangThaiKeTiep =
                    TrangThaiHoaDon.trangThaiTiepTheo(
                            trangThaiHienTai, hinhThucNhan == HinhThucNhan.GIAO_HANG);
            if (trangThaiKeTiep == null || request.getTrangThai() != trangThaiKeTiep.getMa()) {
                throw AppException.badRequest(
                        "Chỉ được chuyển sang trạng thái kế tiếp của đơn hàng.");
            }
        }

        if (com.smashstep.datn.storefront.checkout.OnlineOrderInventory.managed(entityManager, hoaDon))
            request.setIdNhanVien(com.smashstep.datn.sales.service.PosActor.employeeId());
        Long idNhanVienThaoTac = request.getIdNhanVien();
        if (idNhanVienThaoTac == null && hoaDon.getIdNhanVien() != null) {
            idNhanVienThaoTac = hoaDon.getIdNhanVien().getId();
        }
        if (request.getIdNhanVien() != null) {
            NhanVien nhanVien =
                    nhanVienRepository
                            .findById(idNhanVienThaoTac)
                            .orElseThrow(() -> AppException.notFound("Không tìm thấy nhân viên"));
            if (!Integer.valueOf(1).equals(nhanVien.getTrangThai())) {
                throw AppException.badRequest("Nhân viên thao tác phải đang hoạt động");
            }
            // Actor is recorded in history; the invoice owner remains unchanged.
        }
        if (request.getTrangThai() == 6 && com.smashstep.datn.invoice.rules.PosDraftRules.daTruKho(hoaDon))
            com.smashstep.datn.sales.service.TonKhoHoaDonCho.hoanKhiHuy(entityManager, hoaDon);
        if (request.getTrangThai() == 6)
            com.smashstep.datn.storefront.checkout.OnlineOrderInventory.restore(entityManager, hoaDon);
        if (request.getTrangThai() == 5)
            com.smashstep.datn.storefront.checkout.OnlineOrderInventory.collectCodOnCompletion(entityManager, hoaDon);
        hoaDon.setTrangThai(request.getTrangThai());
        hoaDon.setNgayCapNhat(LocalDateTime.now());

        hoaDonRepository.save(hoaDon);
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setIdHoaDon(hoaDon);
        lichSu.setNguoiTao(idNhanVienThaoTac);
        lichSu.setTrangThai(request.getTrangThai());
        lichSu.setGhiChu(
                "POS".equals(request.getNguonThaoTac())
                        && com.smashstep.datn.invoice.rules.PosDraftRules.managed(hoaDon)
                        ? com.smashstep.datn.invoice.rules.PosHistoryRules.THAO_TAC_POS
                        + java.util.Objects.toString(request.getGhiChu(), "")
                        : request.getGhiChu());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSuHoaDonRepository.save(lichSu);
        if(orderMail!=null && !java.util.Objects.equals(trangThaiHienTai,hoaDon.getTrangThai()))orderMail.queueStatus(hoaDon);
        return chiTiet(ma);
    }

    @Transactional
    public HoaDonDetailDto xuLyGiaoThatBai(String ma, com.smashstep.datn.invoice.dto.XuLyGiaoThatBaiDto request) {
        dichVuMaHoaDon.lockInvoiceCreation();
        var h=hoaDonRepository.findByMaHoaDonForUpdate(ma).orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn"));
        Integer previousStatus=h.getTrangThai();
        Long actor=com.smashstep.datn.sales.service.PosActor.employeeId();
        var employee=nhanVienRepository.findById(actor).orElseThrow(() -> AppException.forbidden("Không tìm thấy nhân viên"));
        if (!Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.forbidden("Nhân viên không hoạt động");
        if ((!Integer.valueOf(9).equals(h.getTrangThai()) && !Integer.valueOf(7).equals(h.getTrangThai())) || HoaDonRules.resolveHinhThucNhan(h)!=HinhThucNhan.GIAO_HANG)
            throw AppException.conflict("Chỉ xử lý hàng trả/hoàn/hủy trong nhánh giao thất bại và hoàn tiền");
        long count=com.smashstep.datn.invoice.rules.DeliveryFailureRules.attempts(lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(h.getId()));
        if (request.getSoLanThatBai()==null || request.getSoLanThatBai()!=count) throw AppException.conflict("Hóa đơn đã thay đổi; tải lại trước khi xác nhận");
        if (request.getThaoTac()==null || !java.util.Set.of("HANG_TRA","HOAN_TIEN","HUY").contains(request.getThaoTac())
                || request.getGhiChu()==null || request.getGhiChu().isBlank() || request.getGhiChu().length()>700)
            throw AppException.badRequest("Chọn thao tác và nhập ghi chú xử lý (tối đa 700 ký tự)");
        var rows=lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(h.getId());
        boolean goodsHandled=com.smashstep.datn.invoice.rules.ReturnedGoodsRules.handled(rows);
        if("HANG_TRA".equals(request.getThaoTac())) {
            if(!Integer.valueOf(9).equals(h.getTrangThai())) throw AppException.conflict("Đơn không còn ở bước giao hàng thất bại");
            if(goodsHandled) throw AppException.conflict("Đã xử lý hàng trả; không được nhập kho lần hai");
            if(!request.isDaNhanLaiHang()) throw AppException.badRequest("Phải xác nhận đã nhận lại đầy đủ hàng");
            if(request.isNhapLaiKho()) {
                var lines=hoaDonChiTietRepository.findByIdHoaDonIdOrderByIdAsc(h.getId());
                var quantities=com.smashstep.datn.sales.service.TonKhoHoaDonCho.soLuong(lines);
                var variants=com.smashstep.datn.sales.service.TonKhoHoaDonCho.khoaBienThe(entityManager,quantities.keySet());
                if(variants.size()!=quantities.size()) throw AppException.conflict("Không tìm thấy đủ biến thể để hoàn kho");
                for(var e:quantities.entrySet()) {
                    var v=variants.get(e.getKey());
                    if(e.getValue()<=0 || v.getSoLuong()==null || (long)v.getSoLuong()+e.getValue()>Integer.MAX_VALUE) throw AppException.conflict("Số lượng hoàn kho không hợp lệ");
                    v.setSoLuong(v.getSoLuong()+e.getValue()); v.setNgayCapNhat(LocalDateTime.now());
                }
            }
            ghiXuLy(h,actor,null,com.smashstep.datn.invoice.rules.ReturnedGoodsRules.PREFIX
                    +(request.isNhapLaiKho()?"RESTOCK|Đã nhận lại hàng và nhập lại toàn bộ kho; ":"NO_RESTOCK|Đã nhận lại hàng, không nhập kho do không đủ điều kiện; ")
                    +request.getGhiChu().trim());
            h.setTrangThai(7);
            ghiXuLy(h,actor,7,"Đã xử lý hàng trả; chuyển sang bước Hoàn tiền, chờ đối soát khoản hoàn nếu có tiền đã thu");
        } else {
            if(!Integer.valueOf(7).equals(h.getTrangThai())) throw AppException.conflict("Cần chuyển sang bước Hoàn tiền sau khi xử lý hàng trả");
            if(!goodsHandled) throw AppException.conflict("Cần hoàn tất xử lý hàng trả trước");
            var payments=lichSuThanhToanRepository.findByIdHoaDonIdOrderByThoiGianDesc(h.getId());
            var due=com.smashstep.datn.invoice.rules.DeliveryFailureRules.refundDue(h,payments);
            if("HOAN_TIEN".equals(request.getThaoTac())) {
                if(due.signum()<=0) throw AppException.conflict("Không còn khoản tiền cần hoàn");
                if(request.getSoTienHoan()==null || request.getSoTienHoan().compareTo(due)!=0)
                    throw AppException.conflict("Số tiền cần hoàn đã thay đổi; tải lại và đối soát trước khi xác nhận");
                request.setMaHoanTien(com.smashstep.datn.invoice.rules.RefundReferenceRules.normalize(request.getMaHoanTien()));
                if(!request.isDaHoanTien())
                    throw AppException.badRequest("Phải xác nhận đã hoàn tiền thực tế cho khách");
                var refund=new com.smashstep.datn.invoice.entity.LichSuThanhToan();
                refund.setIdHoaDon(h); refund.setSoTien(due); refund.setTrangThai(2);
                refund.setIdPhuongThucThanhToan(h.getIdPhuongThucThanhToan());
                refund.setMaGiaoDich("REFUND-"+h.getId()+"-"+java.util.UUID.randomUUID());
                refund.setMoTa("Nhân viên " + actor + " xác nhận đã hoàn thực tế; chứng từ: " + request.getMaHoanTien().trim());
                refund.setThoiGian(LocalDateTime.now()); entityManager.persist(refund);
                ghiXuLy(h,actor,7,"Đã xác nhận hoàn tiền thực tế: "+due.toPlainString()+" đ; chứng từ "+request.getMaHoanTien().trim());

            } else {
                if(due.signum()>0) throw AppException.conflict("Chưa hoàn đủ tiền; hoàn tiền trước khi hủy hóa đơn");
                com.smashstep.datn.storefront.checkout.OnlineOrderInventory.releaseVoucher(entityManager,h);
                h.setTrangThai(6);
                ghiXuLy(h,actor,6,"Hủy sau khi đã xử lý hàng trả và đối soát tiền; "+request.getGhiChu().trim());
            }
        }
        h.setNgayCapNhat(LocalDateTime.now());
        if(orderMail!=null && !java.util.Objects.equals(previousStatus,h.getTrangThai()))orderMail.queueStatus(h);
        entityManager.flush();
        return chiTiet(ma);
    }
    private void ghiXuLy(HoaDon h,Long actor,Integer status,String note) {
        var row=new LichSuHoaDon(); row.setIdHoaDon(h); row.setNguoiTao(actor);
        row.setTrangThai(status); row.setGhiChu(note); row.setNgayTao(LocalDateTime.now()); entityManager.persist(row);
    }

    private List<LichSuHoaDonDto> layLichSu(Long idHoaDon) {
        HoaDon hoaDon =
                hoaDonRepository
                        .findById(idHoaDon)
                        .orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn"));
        HinhThucNhan hinhThucNhan = HoaDonRules.resolveHinhThucNhan(hoaDon);
        final HinhThucNhan finalHinhThucNhan = hinhThucNhan;

        return com.smashstep.datn.invoice.rules.PosHistoryRules.hienThiRutGon(
                        hoaDon, lichSuHoaDonRepository.findByIdHoaDonIdOrderByNgayTaoDesc(idHoaDon)).stream()
                // Đơn nhận tại quầy không có các trạng thái của quy trình giao hàng.
                .filter(
                        lichSu ->
                                finalHinhThucNhan == HinhThucNhan.GIAO_HANG
                                        || lichSu.getTrangThai() == null
                                        || lichSu.getTrangThai()
                                        == TrangThaiHoaDon.CHO_XAC_NHAN.getMa()
                                        || lichSu.getTrangThai()
                                        == TrangThaiHoaDon.DA_XAC_NHAN.getMa()
                                        || lichSu.getTrangThai()
                                        == TrangThaiHoaDon.HOAN_THANH.getMa()
                                        || lichSu.getTrangThai() == TrangThaiHoaDon.DA_HUY.getMa()
                                        || lichSu.getTrangThai()
                                        == TrangThaiHoaDon.HOAN_TIEN.getMa()
                                        || lichSu.getTrangThai()
                                        == TrangThaiHoaDon.HOA_DON_CHO.getMa())
                .map(
                        lichSu -> {
                            NhanVien nhanVien = null;
                            if (lichSu.getNguoiTao() != null) {
                                nhanVien =
                                        nhanVienRepository
                                                .findById(lichSu.getNguoiTao())
                                                .orElse(null);
                            }
                            return LichSuHoaDonDto.from(lichSu, nhanVien);
                        })
                .toList();
    }

    private HoaDon timHoaDon(String ma) {
        if (ma == null || ma.isBlank()) {
            throw AppException.badRequest("Mã hóa đơn không được để trống");
        }
        HoaDon found =
                hoaDonRepository
                        .findByMaHoaDon(ma.trim())
                        .orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn: " + ma));
        checkDraftOwner(found);
        if (com.smashstep.datn.invoice.rules.HoaDonHienThiRules.choThanhToanOnline(found))
            throw AppException.notFound("Đơn online chưa thanh toán thành công nên chưa hiển thị ở quản lý");
        return found;
    }

    private void checkDraftOwner(HoaDon h) {
        if (!com.smashstep.datn.invoice.rules.PosDraftRules.managed(h)) return;
        Long actor = com.smashstep.datn.sales.service.PosActor.employeeId();
        if (h.getNgayThanhToan() == null
                && (h.getIdNhanVien() == null || !actor.equals(h.getIdNhanVien().getId())))
            throw AppException.forbidden("Hóa đơn chờ thuộc nhân viên khác");
        var employee =
                nhanVienRepository
                        .findById(actor)
                        .orElseThrow(() -> AppException.forbidden("Nhân viên không tồn tại"));
        if (!Integer.valueOf(1).equals(employee.getTrangThai()))
            throw AppException.forbidden("Nhân viên không hoạt động");
    }

    private Integer getTrangThai(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return null;
        }
        TrangThaiHoaDon value = TrangThaiHoaDon.phanTich(trangThai);
        return value == null ? null : value.getMa();
    }

    private Integer getLoaiDon(String loaiDon) {
        if (loaiDon == null || loaiDon.isBlank()) {
            return null;
        }
        LoaiHoaDon loai = LoaiHoaDon.phanTich(loaiDon);
        return loai == null ? null : loai.getMa();
    }

    private Specification<HoaDon> taoBoLoc(
            String ma, LocalDate tuNgay, LocalDate denNgay, Integer trangThai, Integer loaiHoaDon) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(com.smashstep.datn.invoice.rules.HoaDonHienThiRules.daPhatHanh(root, cb));
            if (ma != null && !ma.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("maHoaDon")),
                                "%" + ma.trim().toLowerCase() + "%"));
            }
            if (tuNgay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ngayTao"), tuNgay.atStartOfDay()));
            }
            if (denNgay != null) {
                predicates.add(
                        cb.lessThan(root.get("ngayTao"), denNgay.plusDays(1).atStartOfDay()));
            }
            if (trangThai != null) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai));
            }
            if (loaiHoaDon != null) {
                predicates.add(cb.equal(root.get("loaiHoaDon"), loaiHoaDon));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
