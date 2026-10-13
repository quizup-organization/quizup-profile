package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.ImageReferenceCount;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileQueryHandlerTest {

    private final ProfileRepositoryPort profileRepository = mock(ProfileRepositoryPort.class);
    private final ProfileQueryHandler handler = new ProfileQueryHandler(profileRepository);

    @Test
    void image_references_delegates_to_repository() {
        List<String> urls = List.of("https://media.quizup.cnadjim.fr/quizup-media/uploads/a.jpg");
        List<ImageReferenceCount> counts = List.of(new ImageReferenceCount(urls.getFirst(), 2));
        when(profileRepository.countReferencesByBannerUrls(urls)).thenReturn(counts);

        assertThat(handler.handle(new ProfileQuery.CountImageReferencesQuery(urls)))
                .containsExactlyElementsOf(counts);
        verify(profileRepository).countReferencesByBannerUrls(urls);
    }
}
