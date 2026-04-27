package ru.practicum.iteractionapi.feignapi.eventfeignclient.compilation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.CompilationDto;
import ru.practicum.iteractionapi.dto.event.NewCompilationDto;
import ru.practicum.iteractionapi.dto.event.UpdateCompilationRequest;

@FeignClient(name = "event-service", contextId = "AdminCompilationFeignClient")
public interface AdminCompilationFeignClient {
	@PostMapping("/admin/compilations")
	@ResponseStatus(HttpStatus.CREATED)
	CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto dto);

	@DeleteMapping("/admin/compilations/{compId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteCompilation(@PathVariable(name = "compId") @Positive Long compId);

	@PatchMapping("/admin/compilations/{compId}")
	CompilationDto updateCompilation(@PathVariable(name = "compId") @Positive Long compId,
									 @RequestBody @Valid UpdateCompilationRequest request);
}
