package io.github.tn1.server.facade.feed;

import java.util.List;
import java.util.stream.Collectors;

import io.github.tn1.server.dto.feed.response.CarrotResponse;
import io.github.tn1.server.dto.feed.response.FeedResponse;
import io.github.tn1.server.dto.feed.response.GroupResponse;
import io.github.tn1.server.entity.feed.Feed;
import io.github.tn1.server.entity.feed.group.Group;
import io.github.tn1.server.entity.feed.medium.FeedMedium;
import io.github.tn1.server.entity.feed.medium.FeedMediumRepository;
import io.github.tn1.server.entity.feed.tag.Tag;
import io.github.tn1.server.entity.feed.tag.TagRepository;
import io.github.tn1.server.entity.like.LikeRepository;
import io.github.tn1.server.entity.user.User;
import io.github.tn1.server.utils.fcm.FcmUtil;
import io.github.tn1.server.utils.s3.S3Util;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedFacade {

	private final S3Util s3Util;
	private final FcmUtil fcmUtil;
	private final LikeRepository likeRepository;
	private final FeedMediumRepository feedMediumRepository;
	private final TagRepository tagRepository;

	public FeedResponse feedToFeedResponse(Feed feed, User user) {
		FeedMedium medium = feedMediumRepository
				.findTopByFeedOrderById(feed);
		FeedResponse response = FeedResponse.builder()
				.feedId(feed.getId())
				.title(feed.getTitle())
				.description(feed.getDescription())
				.price(feed.getPrice())
				.tags(queryTag(feed))
				.medium(medium != null ? s3Util.getObjectUrl(medium.getFileName()) : null)
				.count(feed.getLikes().size())
				.lastModifyDate(feed.getUpdatedDate())
				.isUsedItem(feed.isUsedItem())
				.writerEmail(feed.getUser().getEmail())
				.writerName(feed.getUser().getName())
				.build();
		if (!feed.isUsedItem()) {
			response.setGroupFeed(
					feed.getGroup().getHeadCount(),
					feed.getGroup().getCurrentCount(),
					feed.getGroup().getRecruitmentDate()
			);
		}
		if(user != null)
			response.setLike(likeRepository.findByUserAndFeed(user, feed).isPresent());
		return response;
	}

	public CarrotResponse feedToCarrotResponse(Feed feed, User user) {
		FeedMedium medium = feedMediumRepository
				.findTopByFeedOrderById(feed);
		CarrotResponse response;
		response = CarrotResponse.builder()
				.feedId(feed.getId())
				.title(feed.getTitle())
				.price(feed.getPrice())
				.count(feed.getCount())
				.medium(medium != null ? s3Util.getObjectUrl(medium.getFileName()) : null)
				.tags(tagRepository.findByFeedOrderById(feed)
						.stream().map(Tag::getTag).collect(Collectors.toList()))
				.build();
		if(user != null) {
			response.setLike(likeRepository.findByUserAndFeed(user, feed)
					.isPresent());
		}
		return response;
	}

	public GroupResponse feedToGroupResponse(Feed feed, User user) {
		FeedMedium medium = feedMediumRepository
				.findTopByFeedOrderById(feed);
		GroupResponse response;
		Group group = feed.getGroup();
		response = GroupResponse.builder()
				.feedId(feed.getId())
				.title(feed.getTitle())
				.price(feed.getPrice())
				.count(feed.getLikes().size())
				.medium(medium != null ? s3Util.getObjectUrl(medium.getFileName()) : null)
				.tags(tagRepository.findByFeedOrderById(feed)
						.stream().map(Tag::getTag).collect(Collectors.toList()))
				.currentHeadCount(group.getCurrentCount())
				.headCount(group.getHeadCount())
				.date(group.getRecruitmentDate())
				.build();
		if(user != null) {
			response.setLike(likeRepository.findByUserAndFeed(user, feed)
					.isPresent());
		}
		return response;
	}

	public void addTag(String tag, Feed feed) {
		tagRepository.save(
				Tag.builder()
						.feed(feed)
						.tag(tag)
						.build()
		);
		fcmUtil.sendTagNotification(tag, feed);
	}

	public List<String> queryTag(Feed feed) {
		return tagRepository.findByFeedOrderById(feed)
				.stream().map(Tag::getTag)
				.collect(Collectors.toList());
	}

	public String getFeedPhotoUrl(Feed feed) {
		FeedMedium medium = feedMediumRepository
				.findTopByFeedOrderById(feed);

		return s3Util.getObjectUrl(medium.getFileName());
	}

}
