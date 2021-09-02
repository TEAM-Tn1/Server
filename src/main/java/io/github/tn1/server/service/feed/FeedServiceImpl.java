package io.github.tn1.server.service.feed;

import io.github.tn1.server.dto.feed.request.ModifyCarrotRequest;
import io.github.tn1.server.dto.feed.request.PostCarrotRequest;
import io.github.tn1.server.dto.feed.response.CarrotFeedResponse;
import io.github.tn1.server.dto.feed.response.WriteFeedResponse;
import io.github.tn1.server.entity.feed.Feed;
import io.github.tn1.server.entity.feed.FeedRepository;
import io.github.tn1.server.entity.feed.medium.FeedMedium;
import io.github.tn1.server.entity.feed.medium.FeedMediumRepository;
import io.github.tn1.server.entity.feed.tag.Tag;
import io.github.tn1.server.entity.feed.tag.TagRepository;
import io.github.tn1.server.entity.like.LikeRepository;
import io.github.tn1.server.entity.user.User;
import io.github.tn1.server.entity.user.UserRepository;
import io.github.tn1.server.exception.*;
import io.github.tn1.server.security.facade.UserFacade;
import io.github.tn1.server.utils.fcm.FcmService;
import io.github.tn1.server.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FcmService fcmService;
    private final S3Service s3Service;
    private final FeedRepository feedRepository;
    private final LikeRepository likeRepository;
    private final FeedMediumRepository feedMediumRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Override
    public void postCarrotFeed(PostCarrotRequest request) {

        if(request.getTags() != null && request.getTags().size() > 5)
            throw new TooManyTagsException();

        User user = userRepository.findById(UserFacade.getEmail())
                .orElseThrow(CredentialsNotFoundException::new);

        Feed feed = feedRepository.save(
                Feed.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .price(request.getPrice())
                        .user(user)
                        .isUsedItem(true)
                        .build()
        );


        request.getTags().forEach(tag ->{
                    tagRepository.save(
                            Tag.builder()
                                    .feed(feed)
                                    .tag(tag)
                                    .build()
                    );
                    fcmService.sendTagNotification(tag, feed);
                }
        );

    }

    @Override
    public void modifyCarrotFeed(ModifyCarrotRequest request) {
        User user = userRepository.findById(UserFacade.getEmail())
                .orElseThrow(CredentialsNotFoundException::new);

        Feed feed = feedRepository.findById(request.getFeedId())
                .orElseThrow(FeedNotFoundException::new);

        if(!feed.getUser().getEmail().equals(user.getEmail()))
            throw new NotYourFeedException();

        feed
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setPrice(request.getPrice());

        feedRepository.save(feed);
    }

    @Override
    public List<CarrotFeedResponse> getCarrotFeed(int page, int range) {

        User user = userRepository.findById(UserFacade.getEmail())
                .orElse(null);

        return feedRepository.findByIsUsedItem(true, PageRequest.of(page, range, Sort.by("id").descending()))
                .stream()
                .map(feed -> {
                    FeedMedium medium = feedMediumRepository
                            .findTopByFeedOrderById(feed);
                    CarrotFeedResponse response;
                    response = CarrotFeedResponse.builder()
                            .feedId(feed.getId())
                            .title(feed.getTitle())
                            .price(feed.getPrice())
                            .lastModifyDate(feed.getUpdatedDate())
                            .count(feed.getLikes().size())
                            .description(feed.getDescription())
                            .photo(medium != null ? medium.getPath() : null)
                            .tags(tagRepository.findByFeedOrderById(feed)
                                    .stream().map(Tag::getTag).collect(Collectors.toList()))
                            .build();
                    if(user != null) {
                        response.setLike(likeRepository.findByUserAndFeed(user, feed)
                                .isPresent());
                    }
                    return response;
                }).collect(Collectors.toList());
    }

    @Override
    public void removeFeed(Long id) {
        User user = userRepository.findById(UserFacade.getEmail())
                .orElseThrow(CredentialsNotFoundException::new);

        Feed feed = feedRepository.findById(id)
                .orElseThrow(FeedNotFoundException::new);

        if(!feed.getUser().getEmail().equals(user.getEmail()))
            throw new NotYourFeedException();

        feedRepository.deleteById(id);

    }

    @Override
    public List<WriteFeedResponse> getWriteFeed(String email) {
        User writer = userRepository.findById(email)
                .orElseThrow(UserNotFoundException::new);

        User user = userRepository.findById(UserFacade.getEmail())
                .orElse(null);

        return feedRepository.findByUser(writer)
                .stream().map(feed -> {
                    FeedMedium medium = feedMediumRepository
                            .findTopByFeedOrderById(feed);
                    WriteFeedResponse response = WriteFeedResponse.builder()
                            .feedId(feed.getId())
                            .title(feed.getTitle())
                            .description(feed.getDescription())
                            .price(feed.getPrice())
                            .tags(
                                    tagRepository.findByFeedOrderById(feed)
                                            .stream().map(Tag::getTag).collect(Collectors.toList()))
                            .photo(medium != null ? medium.getPath() : null)
                            .count(feed.getLikes().size())
                            .lastModifyDate(feed.getUpdatedDate())
                            .isUsedItem(feed.isUsedItem())
                            .build();
                    if (!feed.isUsedItem()) {
                        response.setGroupFeed(
                                feed.getGroup().getHeadCount(),
                                feed.getGroup().getRecruitmentDate()
                        );
                    }
                    if(user != null)
                        response.setLike(likeRepository.findByUserAndFeed(user, feed).isPresent());
                    return response;
                }).collect(Collectors.toList());
    }

    @Override
    public void uploadPhoto(List<MultipartFile> files, Long feedId) {
        User user = userRepository.findById(UserFacade.getEmail())
                .orElseThrow(CredentialsNotFoundException::new);

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(FeedNotFoundException::new);

        if(!feed.getUser().getEmail().equals(user.getEmail()))
            throw new NotYourFeedException();

        if(files == null)
            throw new FileEmptyException();

        if(files.size() + feedMediumRepository.countByFeed(feed) > 5)
            throw new TooManyFilesException();

        files.forEach(file ->
                    feedMediumRepository.save(FeedMedium.builder()
                            .feed(feed)
                            .path(s3Service.upload(file))
                            .build())
                );
    }

}
