package ru.practicum.iteractionapi.feignapi.eventfeignclient.compilation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.iteractionapi.dto.event.CompilationDto;

import java.util.List;

@FeignClient(name = "event-service")
@RequestMapping("/compilations")
public interface PublicCompilationFeignClient {
	@GetMapping
	List<CompilationDto> getCompilationsByParam(@RequestParam(value = "pinned", required = false) Boolean pinned,
												@RequestParam(value = "from", defaultValue = "0") Integer from,
												@RequestParam(value = "size", defaultValue = "10") Integer size,
												HttpServletRequest request);

	@GetMapping("/{compId}")
	CompilationDto getCompilationById(@PathVariable(value = "compId") Long compId,
									  HttpServletRequest request);
}
