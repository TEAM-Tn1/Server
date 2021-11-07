package io.github.tn1.server.dto.feed.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedPreviewResponse {

	@JsonUnwrapped
	private final DefaultFeedResponse defaultFeedResponse;

	private int headCount;
	private int currentHeadCount;
	private LocalDate date;

	public void setGroupInformation(int headCount, int currentHeadCount,
			LocalDate date) {
		this.headCount = headCount;
		this.currentHeadCount = currentHeadCount;
		this.date = date;
	}

}
