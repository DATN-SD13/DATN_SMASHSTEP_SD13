package com.smashstep.datn.chat.service;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.chat.dto.ChatDto.*;
import com.smashstep.datn.chat.entity.ChatMessage;
import com.smashstep.datn.chat.entity.ChatSession;
import com.smashstep.datn.chat.repository.ChatMessageRepository;
import com.smashstep.datn.chat.repository.ChatSessionRepository;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ChatService {
    public static final String AI_ACTIVE = "AI_ACTIVE";
    public static final String WAITING_STAFF = "WAITING_STAFF";
    public static final String STAFF_ACTIVE = "STAFF_ACTIVE";
    public static final String CLOSED = "CLOSED";

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ChatAiService aiService;
    private final ObjectMapper objectMapper;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       KhachHangRepository khachHangRepository,
                       NhanVienRepository nhanVienRepository,
                       ChatAiService aiService,
                       ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionDetail currentCustomerSession() {
        KhachHang customer = currentCustomer();
        ChatSession session = sessionRepository
                .findFirstByCustomerIdOrderByUpdatedAtDesc(customer.getId())
                .orElseGet(() -> createAiSession(customer));
        return detail(session);
    }

    @Transactional
    public SessionDetail startNewCustomerSession() {
        KhachHang customer = currentCustomer();
        sessionRepository.findFirstByCustomerIdAndStatusNotOrderByUpdatedAtDesc(customer.getId(), CLOSED)
                .ifPresent(s -> { throw AppException.conflict("Bạn đang có một phiên hỗ trợ chưa kết thúc"); });
        return detail(createAiSession(customer));
    }

    @Transactional
    public SessionDetail customerSend(String content) {
        KhachHang customer = currentCustomer();
        ChatSession session = activeSessionFor(customer.getId());
        ensureOpen(session);

        String cleaned = clean(content);
        saveMessage(session, "CUSTOMER", customer.getId(), customer.getTenKhachHang(), cleaned, null);

        if (AI_ACTIVE.equals(session.getStatus())) {
            List<Long> recentProductIds = recentProductIds(session.getId());
            ChatAiService.AiReply reply = aiService.reply(cleaned, recentProductIds);
            saveAiMessage(session, reply);
        }

        touch(session);
        return detail(session);
    }

    @Transactional
    public SessionDetail requestStaff() {
        KhachHang customer = currentCustomer();
        ChatSession session = activeSessionFor(customer.getId());
        ensureOpen(session);

        if (STAFF_ACTIVE.equals(session.getStatus())) return detail(session);
        if (!WAITING_STAFF.equals(session.getStatus())) {
            session.setStatus(WAITING_STAFF);
            session.setRequestedAt(LocalDateTime.now());
            touch(session);
            saveMessage(session, "SYSTEM", null, "SmashStep",
                    "Yêu cầu hỗ trợ trực tiếp đã được gửi. Nhân viên sẽ tiếp tục cuộc trò chuyện từ nội dung hiện tại.", null);
        }
        return detail(session);
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> staffSessions(boolean includeClosed) {
        currentEmployee();
        return sessionRepository.findForStaff(includeClosed).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public SessionDetail staffDetail(Long sessionId) {
        currentEmployee();
        return detail(findSession(sessionId));
    }

    @Transactional
    public SessionDetail accept(Long sessionId) {
        NhanVien employee = currentEmployee();
        ChatSession session = findSession(sessionId);
        ensureOpen(session);

        if (session.getAssignedEmployee() != null
                && !session.getAssignedEmployee().getId().equals(employee.getId())) {
            throw AppException.conflict("Phiên chat đã được nhân viên khác tiếp nhận");
        }
        if (!WAITING_STAFF.equals(session.getStatus()) && !STAFF_ACTIVE.equals(session.getStatus())) {
            throw AppException.badRequest("Khách hàng chưa yêu cầu nhân viên hỗ trợ");
        }

        session.setAssignedEmployee(employee);
        session.setStatus(STAFF_ACTIVE);
        touch(session);
        saveMessage(session, "SYSTEM", null, "SmashStep",
                employee.getTenNhanVien() + " đã tiếp nhận cuộc trò chuyện.", null);
        return detail(session);
    }

    @Transactional
    public SessionDetail staffSend(Long sessionId, String content) {
        NhanVien employee = currentEmployee();
        ChatSession session = findSession(sessionId);
        ensureAssigned(session, employee);
        saveMessage(session, "STAFF", employee.getId(), employee.getTenNhanVien(), clean(content), null);
        touch(session);
        return detail(session);
    }

    @Transactional
    public SessionDetail close(Long sessionId) {
        NhanVien employee = currentEmployee();
        ChatSession session = findSession(sessionId);
        ensureAssigned(session, employee);
        session.setStatus(CLOSED);
        session.setClosedAt(LocalDateTime.now());
        touch(session);
        saveMessage(session, "SYSTEM", null, "SmashStep",
                "Phiên hỗ trợ đã kết thúc. Bạn có thể bắt đầu cuộc trò chuyện mới bất cứ lúc nào.", null);
        return detail(session);
    }

    private ChatSession createAiSession(KhachHang customer) {
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setCustomer(customer);
        session.setStatus(AI_ACTIVE);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session = sessionRepository.save(session);
        saveAiMessage(session, aiService.welcome());
        return session;
    }

    private ChatSession activeSessionFor(Long customerId) {
        return sessionRepository.findFirstByCustomerIdAndStatusNotOrderByUpdatedAtDesc(customerId, CLOSED)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiên chat đang hoạt động"));
    }

    private ChatSession findSession(Long id) {
        return sessionRepository.findDetailById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiên chat"));
    }

    private void ensureOpen(ChatSession session) {
        if (CLOSED.equals(session.getStatus())) throw AppException.conflict("Phiên chat đã kết thúc");
    }

    private void ensureAssigned(ChatSession session, NhanVien employee) {
        ensureOpen(session);
        if (!STAFF_ACTIVE.equals(session.getStatus()) || session.getAssignedEmployee() == null) {
            throw AppException.badRequest("Hãy tiếp nhận phiên chat trước khi phản hồi");
        }
        if (!session.getAssignedEmployee().getId().equals(employee.getId())) {
            throw AppException.forbidden("Phiên chat đang do nhân viên khác phụ trách");
        }
    }

    private ChatMessage saveAiMessage(ChatSession session, ChatAiService.AiReply reply) {
        AiMessageMeta meta = new AiMessageMeta(
                reply.responseType(),
                reply.products() == null ? List.of() : reply.products(),
                reply.suggestions() == null ? List.of() : reply.suggestions()
        );
        return saveMessage(session, "AI", null, "Tư vấn SmashStep", reply.content(), encodeMeta(meta));
    }

    private ChatMessage saveMessage(ChatSession session, String senderType, Long senderId,
                                    String senderName, String content, String metadataJson) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setMetadataJson(metadataJson);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    private void touch(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private String clean(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isBlank()) throw AppException.badRequest("Nội dung tin nhắn không được để trống");
        if (value.length() > 2000) throw AppException.badRequest("Tin nhắn không được vượt quá 2000 ký tự");
        return value;
    }

    private KhachHang currentCustomer() {
        AuthDetails details = authDetails();
        if (!"KHACH_HANG".equals(details.userType())) throw AppException.forbidden("Chỉ khách hàng được sử dụng chức năng này");
        return khachHangRepository.findById(details.userId())
                .filter(k -> k.getTrangThai() != null && k.getTrangThai() == 1)
                .orElseThrow(() -> AppException.forbidden("Tài khoản khách hàng không hoạt động"));
    }

    private NhanVien currentEmployee() {
        AuthDetails details = authDetails();
        if (!"NHAN_VIEN".equals(details.userType())) throw AppException.forbidden("Chỉ nhân viên được sử dụng chức năng này");
        return nhanVienRepository.findById(details.userId())
                .filter(nv -> nv.getTrangThai() != null && nv.getTrangThai() == 1)
                .orElseThrow(() -> AppException.forbidden("Tài khoản nhân viên không hoạt động"));
    }

    private AuthDetails authDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof AuthDetails details)) {
            throw AppException.unauthorized("Vui lòng đăng nhập để sử dụng chat");
        }
        return details;
    }

    private SessionDetail detail(ChatSession session) {
        List<MessageResponse> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream().map(this::messageResponse).toList();
        return new SessionDetail(summary(session), messages);
    }

    private SessionSummary summary(ChatSession session) {
        String last = messageRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId())
                .map(ChatMessage::getContent).orElse("");
        KhachHang c = session.getCustomer();
        NhanVien e = session.getAssignedEmployee();
        return new SessionSummary(
                session.getId(), c.getId(), c.getMaKhachHang(), displayCustomer(c), c.getSoDienThoai(),
                e == null ? null : e.getId(), e == null ? null : e.getTenNhanVien(),
                session.getStatus(), last, session.getUpdatedAt(), session.getRequestedAt()
        );
    }

    private MessageResponse messageResponse(ChatMessage m) {
        AiMessageMeta meta = decodeMeta(m.getMetadataJson());
        return new MessageResponse(
                m.getId(), m.getSenderType(), m.getSenderId(), m.getSenderName(), m.getContent(),
                meta == null ? null : meta.responseType(),
                meta == null || meta.products() == null ? List.of() : meta.products(),
                meta == null || meta.suggestions() == null ? List.of() : meta.suggestions(),
                m.getCreatedAt()
        );
    }

    /** Lấy các sản phẩm AI vừa giới thiệu gần nhất để hiểu câu kiểu “mẫu đó còn size 41 không?”. */
    private List<Long> recentProductIds(Long sessionId) {
        List<ChatMessage> messages = new ArrayList<>(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
        Collections.reverse(messages);
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (ChatMessage message : messages) {
            if (!"AI".equals(message.getSenderType())) continue;
            AiMessageMeta meta = decodeMeta(message.getMetadataJson());
            if (meta == null || meta.products() == null || meta.products().isEmpty()) continue;
            for (ProductSuggestion product : meta.products()) {
                if (product != null && product.id() != null) result.add(product.id());
                if (result.size() >= 6) return List.copyOf(result);
            }
        }
        return List.copyOf(result);
    }

    private String encodeMeta(AiMessageMeta meta) {
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiMessageMeta decodeMeta(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, AiMessageMeta.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String displayCustomer(KhachHang customer) {
        return customer.getTenKhachHang() == null || customer.getTenKhachHang().isBlank()
                ? "bạn" : customer.getTenKhachHang();
    }
}
