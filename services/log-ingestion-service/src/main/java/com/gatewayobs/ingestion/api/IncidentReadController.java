package com.gatewayobs.ingestion.api;

import com.gatewayobs.ingestion.read.IncidentDetail;
import com.gatewayobs.ingestion.read.IncidentReadService;
import com.gatewayobs.ingestion.read.IncidentSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentReadController {

    private final IncidentReadService incidentReadService;

    public IncidentReadController(IncidentReadService incidentReadService) {
        this.incidentReadService = incidentReadService;
    }

    @GetMapping
    public List<IncidentSummary> list(@RequestParam(name = "limit", defaultValue = "25") int limit) {
        return incidentReadService.recent(limit);
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentDetail> detail(@PathVariable UUID incidentId) {
        return incidentReadService.detail(incidentId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
