package com.bookdhara.safety;

import com.bookdhara.safety.BlockService.BlockedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SafetyController {

    public record ReportRequest(@NotNull Report.TargetType type, @NotNull Long targetId,
                                @NotNull Report.Reason reason, @Size(max = 500) String details) {
    }

    private final BlockService blocks;
    private final ReportService reports;

    public SafetyController(BlockService blocks, ReportService reports) {
        this.blocks = blocks;
        this.reports = reports;
    }

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal Long userId, @Valid @RequestBody ReportRequest r) {
        reports.report(userId, r.type(), r.targetId(), r.reason(), r.details());
    }

    @GetMapping("/api/me/blocks")
    public List<BlockedUser> blocked(@AuthenticationPrincipal Long userId) {
        return blocks.blockedBy(userId);
    }

    @PutMapping("/api/me/blocks/{otherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal Long userId, @PathVariable Long otherId) {
        blocks.block(userId, otherId);
    }

    @DeleteMapping("/api/me/blocks/{otherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthenticationPrincipal Long userId, @PathVariable Long otherId) {
        blocks.unblock(userId, otherId);
    }
}
