package com.techquiz.question;

import static com.techquiz.testsupport.QuestionFixtures.question;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class QuestionSyncServiceTest {

    private static final Path DIR = Path.of("questions");

    @Mock
    QuestionFileLoader loader;
    @Mock
    TopicRepository topics;
    @Mock
    QuestionRepository questions;

    @Test
    void upsertsEverythingThenDeletesWhatTheFilesNoLongerContain() {
        List<Topic> fileTopics = List.of(new Topic("java", "Java", "The language"));
        List<Question> fileQuestions = List.of(question("java-001", "java"), question("java-002", "java"));
        when(loader.load(DIR)).thenReturn(new QuestionCatalog(fileTopics, fileQuestions));
        when(questions.deleteByIdNotIn(List.of("java-001", "java-002"))).thenReturn(3L);
        when(topics.deleteByIdNotIn(List.of("java"))).thenReturn(1L);

        QuestionSyncService.SyncResult result = service(true).sync();

        verify(topics).saveAll(fileTopics);
        verify(questions).saveAll(fileQuestions);
        assertThat(result).isEqualTo(new QuestionSyncService.SyncResult(1, 2, 1, 3));
    }

    @Test
    void syncsOnStartupWhenEnabled() {
        when(loader.load(DIR)).thenReturn(new QuestionCatalog(List.of(), List.of()));

        service(true).run(new DefaultApplicationArguments());

        verify(loader).load(DIR);
    }

    @Test
    void skipsTheStartupSyncWhenDisabled() {
        service(false).run(new DefaultApplicationArguments());

        verifyNoInteractions(loader, topics, questions);
    }

    @Test
    void anInvalidFileStopsTheSyncBeforeAnyWrite() {
        when(loader.load(DIR)).thenThrow(new QuestionFileException("java.yaml: broken"));

        assertThatThrownBy(() -> service(true).run(new DefaultApplicationArguments()))
                .isInstanceOf(QuestionFileException.class);
        verify(questions, never()).saveAll(any());
        verify(questions, never()).deleteByIdNotIn(any());
    }

    private QuestionSyncService service(boolean syncOnStartup) {
        return new QuestionSyncService(loader, topics, questions, new QuestionsProperties(DIR, syncOnStartup));
    }
}
