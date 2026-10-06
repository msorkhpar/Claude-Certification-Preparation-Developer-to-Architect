# Risk register

Every person who receives output is told that AI helped produce it. Disclosure is part of the deployment and not an option.

| Risk | Failure mode | Control | Owner | Residual |
|---|---|---|---|---|
| A confident answer that is wrong | hallucination | grounding-check | Claims quality lead | Low: an unsupported answer is held |
| Instructions hidden in a document | prompt injection | untrusted-content | Platform security lead | Low: untrusted text never reaches the system prompt |
| Personal data in a prompt or a log | privacy leak | input-screen | Data protection officer | Medium: patterns do not find names |
| Different outcomes for different groups | unfair outcome | parity-report | Model risk lead | Medium: monitored and not prevented |
