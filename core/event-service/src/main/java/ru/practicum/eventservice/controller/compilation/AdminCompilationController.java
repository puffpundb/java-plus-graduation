package ru.practicum.eventservice.controller.compilation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.eventservice.service.CompilationService;
import ru.practicum.iteractionapi.dto.event.CompilationDto;
import ru.practicum.iteractionapi.dto.event.NewCompilationDto;
import ru.practicum.iteractionapi.dto.event.UpdateCompilationRequest;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.compilation.AdminCompilationFeignClient;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCompilationController implements AdminCompilationFeignClient {
	final CompilationService compilationService;

	@Override
	public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto dto) {
		log.info("POST /admin/compilations: {}", dto);
		return compilationService.createCompilation(dto);
	}

	@Override
	public void deleteCompilation(@PathVariable(name = "compId") @Positive Long compId) {
		log.info("DELETE /admin/compilations/{}", compId);
		compilationService.deleteCompilation(compId);
	}

	@Override
	public CompilationDto updateCompilation(@PathVariable(name = "compId") @Positive Long compId,
											@RequestBody @Valid UpdateCompilationRequest request) {
		log.info("PATCH /admin/compilations/{}", compId);
		return compilationService.updateCompilation(compId, request);
	}
}
