package com.example.kb_ai_backend.controller;

import com.example.kb_ai_backend.service.WidgetSelectionExperimentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/** Experimental comparison API; never performs financial operations or entity extraction. */
@RestController
@RequestMapping("/api/experiments/widget-selection")
public class WidgetSelectionExperimentController {
    private final WidgetSelectionExperimentService service;
    public WidgetSelectionExperimentController(WidgetSelectionExperimentService service) { this.service = service; }
    public record Request(String message) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of(
                "status", error.getStatusCode().value(), "error", error.getReason()));
    }

    @PostMapping
    public WidgetSelectionExperimentService.Result select(@RequestBody Request request,
            @RequestParam(defaultValue = "jev") String provider) {
        return service.select(request.message(), provider);
    }
}
