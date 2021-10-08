package io.github.tn1.server.dto.admin.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserBlackDateResponse {

	private final LocalDate blackDate;

}
