package io.github.quizup.profile.domain.query;

public interface ProfileQuery {

	record GetProfileByIdQuery(String profileId) implements ProfileQuery {
	}

	record ProfileExistsByIdQuery(String profileId) implements ProfileQuery {
	}
}

