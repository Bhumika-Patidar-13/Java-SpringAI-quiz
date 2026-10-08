document.addEventListener("DOMContentLoaded", () => {

    const form = document.getElementById("answerForm");
    const timer = document.getElementById("timer");
    const timerBox = document.getElementById("timerBox");

    if (!form || !timer) {
        return;
    }

    const TIMER_KEY = "quizRemainingSeconds";
    const DEFAULT_SECONDS = 5 * 60;

    let remaining = Number(sessionStorage.getItem(TIMER_KEY));

    if (!Number.isFinite(remaining) || remaining <= 0) {
        remaining = DEFAULT_SECONDS;
        sessionStorage.setItem(TIMER_KEY, remaining);
    }

    function renderTimer() {
        const minutes = Math.floor(remaining / 60);
        const seconds = remaining % 60;

        timer.textContent =
            String(minutes).padStart(2, "0") +
            ":" +
            String(seconds).padStart(2, "0");

        if (remaining <= 30) {
            timerBox.classList.add("danger");
        }
    }

    function finishByTimeout() {
        sessionStorage.removeItem(TIMER_KEY);
        form.submit();
    }

    renderTimer();

    const interval = setInterval(() => {
        remaining--;
        sessionStorage.setItem(TIMER_KEY, remaining);
        renderTimer();

        if (remaining <= 0) {
            clearInterval(interval);
            finishByTimeout();
        }
    }, 1000);

    form.addEventListener("submit", () => {
        clearInterval(interval);
        sessionStorage.setItem(TIMER_KEY, remaining);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            const active = document.activeElement;

            if (active && active.tagName === "INPUT" && active.type === "text") {
                return;
            }

            event.preventDefault();

            const selected = form.querySelector(
                'input[name="quesAns"]:checked'
            );

            const textInput = form.querySelector(
                'input[name="quesAns"][type="text"]'
            );

            if (selected || (textInput && textInput.value.trim() !== "")) {
                form.requestSubmit();
            }
        }
    });
});
