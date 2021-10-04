package io.github.tn1.server.dto.chat.request;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QueryMessageRequest {

	@NotNull(message = "room_id는 null이면 안됩니다.")
	private String roomId;

}
