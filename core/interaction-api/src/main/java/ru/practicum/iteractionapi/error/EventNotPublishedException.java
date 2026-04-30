package ru.practicum.iteractionapi.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EventNotPublishedException extends RuntimeException {
	public EventNotPublishedException(String message) {
		super(message);
	}
}
