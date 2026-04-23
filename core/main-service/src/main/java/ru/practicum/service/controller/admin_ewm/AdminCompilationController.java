package ru.practicum.service.controller.admin_ewm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.dto.CompilationDto;
import ru.practicum.service.dto.NewCompilationDto;
import ru.practicum.service.dto.UpdateCompilationRequest;
import ru.practicum.service.service.CompilationService;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCompilationController {
    private final CompilationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(
            @RequestBody @Valid NewCompilationDto dto
    ) {
        log.info("POST /admin/compilations: {}", dto);
        return service.createCompilation(dto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(
            @PathVariable(name = "compId") @Positive Long compId
    ) {
        log.info("DELETE /admin/compilations/{}", compId);
        service.deleteCompilation(compId);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(
            @PathVariable(name = "compId") @Positive Long compId,
            @RequestBody @Valid UpdateCompilationRequest request
    ) {
        log.info("PATCH /admin/compilations/{}", compId);
        return service.updateCompilation(compId, request);
    }
}
