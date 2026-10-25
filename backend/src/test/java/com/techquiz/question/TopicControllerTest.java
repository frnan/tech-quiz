package com.techquiz.question;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TopicController.class)
class TopicControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    TopicService topicService;

    @Test
    void listsTopics() throws Exception {
        when(topicService.listTopics()).thenReturn(List.of(new TopicSummary("java", "Java", "The language", 12)));

        mvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":"java","name":"Java","description":"The language","questionCount":12}]
                        """));
    }
}
