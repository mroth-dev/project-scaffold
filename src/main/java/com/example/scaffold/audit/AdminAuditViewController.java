package com.example.scaffold.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only audit trail viewer for the admin section (ADMIN/MANAGER only).
 */
@Controller
@RequestMapping("/admin/audit")
public class AdminAuditViewController {

    private final AuditEventRepository auditEventRepository;

    public AdminAuditViewController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Model model) {
        model.addAttribute("page", auditEventRepository.findByFilters(
                blankToNull(entityType), entityId, blankToNull(eventType), userId, null, null, pageable));
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);
        model.addAttribute("eventType", eventType);
        model.addAttribute("userId", userId);
        return "admin/audit/index";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
