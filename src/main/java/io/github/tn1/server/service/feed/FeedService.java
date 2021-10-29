package io.github.tn1.server.service.feed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import io.github.tn1.server.dto.feed.request.ModifyTagRequest;
import io.github.tn1.server.dto.feed.response.FeedPreviewResponse;
import io.github.tn1.server.dto.feed.response.FeedResponse;
import io.github.tn1.server.dto.feed.response.TagResponse;
import io.github.tn1.server.entity.feed.Feed;
import io.github.tn1.server.entity.feed.FeedRepository;
import io.github.tn1.server.entity.feed.medium.FeedMedium;
import io.github.tn1.server.entity.feed.medium.FeedMediumRepository;
import io.github.tn1.server.entity.feed.tag.TagRepository;
import io.github.tn1.server.entity.like.Like;
import io.github.tn1.server.entity.like.LikeRepository;
import io.github.tn1.server.entity.user.User;
import io.github.tn1.server.entity.user.UserRepository;
import io.github.tn1.server.exception.AlreadyLikedFeedException;
import io.github.tn1.server.exception.CredentialsNotFoundException;
import io.github.tn1.server.exception.FeedNotFoundException;
import io.github.tn1.server.exception.FileEmptyException;
import io.github.tn1.server.exception.LikeNotFoundException;
import io.github.tn1.server.exception.MediumNotFoundException;
import io.github.tn1.server.exception.NotYourFeedException;
import io.github.tn1.server.exception.TooManyFilesException;
import io.github.tn1.server.exception.TooManyTagsException;
import io.github.tn1.server.exception.UserNotFoundException;
import io.github.tn1.server.facade.feed.FeedFacade;
import io.github.tn1.server.facade.user.UserFacade;
import io.github.tn1.server.utils.s3.S3Util;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FeedService {

	private final UserFacade userFacade;
	private final FeedFacade feedFacade;
    private final S3Util s3Util;
    private final FeedRepository feedRepository;
    private final TagRepository tagRepository;
    private final FeedMediumRepository feedMediumRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;

	public FeedResponse queryFeed(Long feedId) {
    	User user = userRepository.findById(userFacade.getEmail())
				.orElse(null);
    	Feed feed = feedRepository.findById(feedId)
				.orElseThrow(FeedNotFoundException::new);
    	return feedFacade.feedToFeedResponse(feed, user);
	}

	@Transactional
	public void uploadPhoto(List<MultipartFile> files, Long feedId) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

		Feed feed = feedRepository.findById(feedId)
				.orElseThrow(FeedNotFoundException::new);

		if(!feed.getUser().matchEmail(user.getEmail()))
			throw new NotYourFeedException();

		if(files == null)
			throw new FileEmptyException();

		if(files.size() + feedMediumRepository.countByFeed(feed) > 5)
			throw new TooManyFilesException();

		files.forEach(file ->
				feedMediumRepository.save(FeedMedium.builder()
						.feed(feed)
						.fileName(s3Util.upload(file))
						.build())
		);

		addRoomPhoto(feed);
	}

	public void removeFeed(Long id) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

		Feed feed = feedRepository.findById(id)
				.orElseThrow(FeedNotFoundException::new);

		if(!feed.getUser().matchEmail(user.getEmail()))
			throw new NotYourFeedException();

		List<String> photoLinks = new ArrayList<>();

		for(FeedMedium medium : feed.getMedia()) {
			photoLinks.add(medium.getFileName());
		}

		photoLinks.forEach(this::removePhoto);

		feedRepository.deleteById(id);

	}

	public TagResponse queryTag(Long feedId) {
    	Feed feed = feedRepository.findById(feedId)
				.orElseThrow(FeedNotFoundException::new);

    	return new TagResponse(
				feedFacade.queryTag(feed)
		);
	}

	@Transactional
	public void modifyTag(ModifyTagRequest request) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

		Feed feed = feedRepository.findById(request.getFeedId())
				.orElseThrow(FeedNotFoundException::new);

		if(!feed.getUser().matchEmail(user.getEmail()))
			throw new NotYourFeedException();

		if(request.getTags().size() > 5)
			throw new TooManyTagsException();

		tagRepository.deleteByFeed(feed);

		for(String tag : request.getTags()) {
			feedFacade.addTag(tag, feed);
		}
	}

	@Transactional
	public void addLike(Long feedId) {
    	Feed feed = feedRepository.findById(feedId)
				.orElseThrow(FeedNotFoundException::new);
    	User user = userRepository
				.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

    	try {
			likeRepository.save(
					Like.builder()
							.user(user)
							.feed(feed)
							.build()
			);
			feed.increaseCount();
		} catch (RuntimeException e) {
    		throw new AlreadyLikedFeedException();
		}
	}

	@Transactional
	public void removeLike(Long feedId) {
		Feed feed = feedRepository.findById(feedId)
				.orElseThrow(FeedNotFoundException::new);
		User user = userRepository
				.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

		likeRepository.delete(
				likeRepository
						.findByUserAndFeed(user, feed)
						.orElseThrow(LikeNotFoundException::new)
		);
		feed.decreaseCount();
	}

	public List<FeedPreviewResponse> queryFeed(int page, int range, String sort, boolean isUsedItem) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElse(null);

		PageRequest pageRequest;

		if(sort != null && sort.equals("like")) {
			pageRequest = PageRequest.of(page, range, Sort.by("count").descending().and(Sort.by("id")));
		} else pageRequest = PageRequest.of(page, range, Sort.by("id").descending());

		return feedRepository.findByIsUsedItem(isUsedItem,
				pageRequest)
				.stream().filter(feed -> feed.getGroup().getCurrentCount() < feed.getGroup().getHeadCount())
				.map(feed ->
						feedFacade.feedToPreviewResponse(feed, user)
				).collect(Collectors.toList());
	}

	public List<FeedPreviewResponse> queryLikeFeed(boolean isUsedItem) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElseThrow(UserNotFoundException::new);
		return user.getLikes()
				.stream().filter(like -> like.getFeed().isUsedItem() == isUsedItem)
				.map(like ->
						feedFacade.feedToPreviewResponse(like.getFeed(), user)
				).collect(Collectors.toList());
	}

	private void removePhoto(String fileName) {
		User user = userRepository.findById(userFacade.getEmail())
				.orElseThrow(CredentialsNotFoundException::new);

		FeedMedium medium = feedMediumRepository.findByFileName(fileName)
				.orElseThrow(MediumNotFoundException::new);

		if(!medium.getFeed().getUser().matchEmail(user.getEmail()))
			throw new NotYourFeedException();

		s3Util.delete(medium.getFileName());
		feedMediumRepository.delete(medium);
	}

	private void addRoomPhoto(Feed feed) {
    	String photoUrl = feedFacade.getFeedPhotoUrl(feed);
    	feed.getRoom().forEach(room -> {
    		if(room.getPhotoUrl() == null)
    			room.initPhotoUrl(photoUrl);
		});
	}

}
