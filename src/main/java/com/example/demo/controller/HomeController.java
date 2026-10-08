package com.example.demo.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Question;

import jakarta.servlet.http.HttpSession;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Controller
public class HomeController {

    private final ChatClient chatClient;

    public HomeController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/start")
    public String startQuiz(HttpSession session) {

        String prompt = """
                [Output ONLY a valid raw JSON array.

                Generate exactly 10 Java quiz questions.

                The questions must include all three types:
                - MCQ
                - FILL_BLANK
                - TRUE_FALSE

                Use a reasonable mixture of all three types.

                Each object must contain exactly these five keys:
                "qno"
                "type"
                "question"
                "options"
                "ans"

                General rules:
                - "qno" must be a sequential integer from 1 to 10.
                - "type" must be exactly one of:
                  "MCQ", "FILL_BLANK", "TRUE_FALSE"
                - Questions must be related to Java.
                - Do not repeat the same question.
                - Each question must have one clear and unambiguous correct answer.

                Rules for MCQ:
                - "type" must be "MCQ".
                - "options" must contain exactly 4 options.
                - All 4 options must be different.
                - There must be exactly one correct option.
                - "ans" must exactly match one of the four options.

                Rules for FILL_BLANK:
                - "type" must be "FILL_BLANK".
                - The question must contain exactly one "____".
                - "options" must be an empty array.
                - "ans" must be a short expected answer.

                Rules for TRUE_FALSE:
                - "type" must be "TRUE_FALSE".
                - "options" must be exactly ["true", "false"].
                - "ans" must be exactly either "true" or "false".

                Do not include explanations.
                Do not include Markdown.
                Do not include text before or after the JSON.
                Return only the valid JSON array.]
                """;

        String response = chatClient.prompt(prompt).call().content();

        ObjectMapper mapper = new ObjectMapper();
        List<Question> list = mapper.readValue(
                response,
                new TypeReference<List<Question>>() {}
        );

        for (Question q : list) {
            q.setMarks(0);
            q.setUserAns("");
        }

        session.setAttribute("list", list);
        session.setAttribute("count", 0);

        return "redirect:/question";
    }

    @GetMapping("/question")
    public String question(HttpSession session, ModelMap model) {

        List<Question> list = getQuiz(session);

        if (list == null || list.isEmpty()) {
            return "redirect:/";
        }

        Integer count = (Integer) session.getAttribute("count");

        if (count == null) {
            count = 0;
            session.setAttribute("count", 0);
        }

        if (count < 0 || count >= list.size()) {
            count = 0;
            session.setAttribute("count", 0);
        }

        model.addAttribute("current_question", list.get(count));
        model.addAttribute("questionIndex", count);
        model.addAttribute("totalQuestions", list.size());

        return "question";
    }

    @PostMapping("/next")
    public String nextQuestion(
            HttpSession session,
            @RequestParam(value = "quesAns", defaultValue = "") String quesAns) {

        List<Question> list = getQuiz(session);

        if (list == null || list.isEmpty()) {
            return "redirect:/";
        }

        Integer count = (Integer) session.getAttribute("count");

        if (count == null) {
            count = 0;
        }

        if (count >= 0 && count < list.size()) {
            Question current = list.get(count);

            current.setUserAns(quesAns == null ? "" : quesAns.trim());

            String correctAnswer = current.getAns() == null
                    ? ""
                    : current.getAns().trim();

            String userAnswer = current.getUserAns() == null
                    ? ""
                    : current.getUserAns().trim();

            current.setMarks(
                    !userAnswer.isEmpty()
                    && correctAnswer.equalsIgnoreCase(userAnswer)
                    ? 1
                    : 0
            );
        }

        if (count == list.size() - 1) {
            int total = calculateTotal(list);

            session.setAttribute("total", total);
            session.setAttribute("quizCompleted", true);

            return "redirect:/result";
        }

        session.setAttribute("count", count + 1);

        return "redirect:/question";
    }

    @GetMapping("/result")
    public String result(HttpSession session, ModelMap model) {

        List<Question> list = getQuiz(session);

        if (list == null || list.isEmpty()) {
            return "redirect:/";
        }

        int total = calculateTotal(list);

        model.addAttribute("total", total);
        model.addAttribute("totalQuestions", list.size());

        return "result";
    }

    @GetMapping("/review")
    public String review(HttpSession session, ModelMap model) {

        List<Question> list = getQuiz(session);

        if (list == null || list.isEmpty()) {
            return "redirect:/";
        }

        model.addAttribute("questions", list);
        model.addAttribute("total", calculateTotal(list));

        return "review";
    }

    private List<Question> getQuiz(HttpSession session) {
        return (List<Question>) session.getAttribute("list");
    }

    private int calculateTotal(List<Question> list) {
        int total = 0;

        for (Question q : list) {
            total += q.getMarks();
        }

        return total;
    }
}
