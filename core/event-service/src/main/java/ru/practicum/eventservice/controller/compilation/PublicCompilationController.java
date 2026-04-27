package ru.practicum.eventservice.controller.compilation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.eventservice.service.CompilationService;
import ru.practicum.iteractionapi.dto.event.CompilationDto;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.compilation.PublicCompilationFeignClient;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/compilations")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PublicCompilationController implements PublicCompilationFeignClient {
	final CompilationService compilationService;

	@Override
	public List<CompilationDto> getCompilationsByParam(@RequestParam(value = "pinned", required = false) Boolean pinned,
													   @RequestParam(value = "from", defaultValue = "0") Integer from,
													   @RequestParam(value = "size", defaultValue = "10") Integer size,
													   HttpServletRequest request) {
		log.info("PublicCompilationController: вызов эндпоинта GET /compilation с параметрами --" +
				" pinned: {}, from: {}, size: {}", pinned, from, size);

		return compilationService.getCompilations(pinned, from, size, request);
	}

	@Override
	public CompilationDto getCompilationById(@PathVariable(value = "compId") Long compId,
											 HttpServletRequest request) {
		log.info("PublicCompilationController: вызов эндпоинта GET /compilation/{}", compId);

		return compilationService.getCompilationById(compId, request);
	}
}
