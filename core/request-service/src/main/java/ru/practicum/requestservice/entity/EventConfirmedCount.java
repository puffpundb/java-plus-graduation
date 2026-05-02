package ru.practicum.requestservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventConfirmedCount {
	private Long eventId;
	private Long count;

}
