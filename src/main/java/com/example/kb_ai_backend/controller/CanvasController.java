package com.example.kb_ai_backend.controller;

import com.example.kb_ai_backend.service.JevCanvasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/canvas")
public class CanvasController {

    private final JevCanvasService canvas;

    public CanvasController(JevCanvasService canvas) {
        this.canvas = canvas;
    }

    public record CanvasRequest(String message) {}

    @PostMapping
    public Map<String, Object> generateCanvas(@RequestBody CanvasRequest request) {

        return canvas.compose(request == null ? null : request.message());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of(
                "status", error.getStatusCode().value(), "error", error.getReason()));
    }
}
