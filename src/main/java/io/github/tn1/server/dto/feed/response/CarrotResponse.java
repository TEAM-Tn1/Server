package io.github.tn1.server.dto.feed.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CarrotResponse {

	private final long feedId;
	private final String title;
	private final int price;
	private final List<String> tags;
	private final String medium;
	private boolean like;
	private final int count;

	public void setLike(boolean like) {
		this.like = like;
	}

}
