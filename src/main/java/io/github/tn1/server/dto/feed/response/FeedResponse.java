package io.github.tn1.server.dto.feed.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedResponse {

    private final Long feedId;
    private final String title;
    private final String description;
    private final Integer price;
    private final List<String> tags;
    private final String photo;
    private final LocalDateTime lastModifyDate;
    private boolean like;
    private final Integer count;

    public void setLike(boolean value) {
        this.like = value;
    }

}
