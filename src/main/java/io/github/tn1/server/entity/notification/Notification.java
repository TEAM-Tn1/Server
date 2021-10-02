package io.github.tn1.server.entity.notification;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.github.tn1.server.entity.user.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_notification")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 45)
	private String title;

	private String message;

	@Column(length = 45)
	private String value;

	@Column(columnDefinition = "BIT(1) default false")
	private boolean isWatch;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "email")
	private User user;

	@Builder
	public Notification(String title, String message,
			String value, boolean isWatch,
			User user) {
		this.title = title;
		this.message = message;
		this.value = value;
		this.isWatch = isWatch;
		this.user = user;
	}

}
