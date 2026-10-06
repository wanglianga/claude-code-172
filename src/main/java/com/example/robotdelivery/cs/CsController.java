package com.example.robotdelivery.cs;

import com.example.robotdelivery.auth.CurrentUserHolder;
import com.example.robotdelivery.auth.RequireRole;
import com.example.robotdelivery.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "客服端", description = "客服工单：异常责任追踪与处理")
@RestController
@RequestMapping("/api/cs")
public class CsController {

    private final CsCaseRepository csCaseRepository;
    private final CsService csService;

    public CsController(CsCaseRepository csCaseRepository, CsService csService) {
        this.csCaseRepository = csCaseRepository;
        this.csService = csService;
    }

    @Operation(summary = "工单列表（按状态/责任/类型过滤）")
    @RequireRole({Role.CS, Role.OPS, Role.ADMIN})
    @GetMapping("/cases")
    public List<CsCase> list(@RequestParam(required = false) CaseStatus status,
                             @RequestParam(required = false) Responsibility responsibility,
                             @RequestParam(required = false) CaseType caseType) {
        List<CsCase> all = csCaseRepository.findAll();
        return all.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> responsibility == null || c.getResponsibility() == responsibility)
                .filter(c -> caseType == null || c.getCaseType() == caseType)
                .collect(Collectors.toList());
    }

    @Operation(summary = "异常责任汇总（客服端追踪异常责任）")
    @RequireRole({Role.CS, Role.OPS, Role.ADMIN})
    @GetMapping("/cases/summary")
    public Map<String, Object> summary() {
        List<CsCase> all = csCaseRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("open", all.stream().filter(c -> c.getStatus() == CaseStatus.OPEN).count());
        result.put("byResponsibility", all.stream().collect(Collectors.groupingBy(
                c -> c.getResponsibility().name(), Collectors.counting())));
        result.put("byCaseType", all.stream().collect(Collectors.groupingBy(
                c -> c.getCaseType().name(), Collectors.counting())));
        return result;
    }

    public record ResolveRequest(@NotNull Responsibility responsibility, String note) {
    }

    @Operation(summary = "工单结案：认定责任归属")
    @RequireRole({Role.CS, Role.ADMIN})
    @PostMapping("/cases/{id}/resolve")
    public CsCase resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return csService.resolve(id, request.responsibility(), request.note(),
                CurrentUserHolder.get().username());
    }
}
