package com.initprep.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final ChatClient chatClient;

    @GetMapping("/test")
    public String test() {

        return chatClient
            .prompt()
            .user("Explain binary search in one sentence.")
            .call()
            .content();
    }

}
