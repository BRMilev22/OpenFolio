package com.openfolio.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Uses local Ollama (qwen2.5:14b) to transform raw GitHub data into
 * professional resume content modeled after Enhancv's writing style.
 *
 * <p>Enhancv conventions applied:
 * <ul>
 *   <li>Summary: third-person, 3-4 sentences, mentions expertise + tech</li>
 *   <li>Project descriptions: 3-5 bullet points, each starting with a past-tense
 *       action verb, mentioning technology, and quantifying impact where possible</li>
 *   <li>Professional, concise, metrics-driven language throughout</li>
 * </ul>
 */
@Service
public class AiResumeEnhancer {

    private static final Logger log = LoggerFactory.getLogger(AiResumeEnhancer.class);

    private final OllamaClient ollama;

    public AiResumeEnhancer(OllamaClient ollama) {
        this.ollama = ollama;
    }

    // ─── Professional summary ────────────────────────────────────────────────

    /**
     * Generate an Enhancv-style professional summary for the top of a resume.
     * Third-person voice, mentions role + tech stack + impact.
     */
    public String enhanceProfessionalSummary(String name,
                                              String rawReadme,
                                              List<String> topLanguages) {
        String system = """
                You are a professional resume writer who writes for Enhancv, a premium resume platform.
                Write a professional summary section for a software engineer's resume.

                STYLE REQUIREMENTS (match Enhancv exactly):
                - Write exactly 3-4 flowing sentences, 60-100 words total.
                - Third person WITHOUT using the person's name. Start with role descriptor.
                  GOOD: "Full-stack engineer with extensive experience in..."
                  BAD: "Boris is a full-stack engineer..." or "I am a full-stack engineer..."
                - First sentence: role title + area of expertise + approximate experience scope.
                  Example: "Full-stack software engineer with extensive experience in web and mobile application development."
                - Second sentence: mention 3-5 specific technologies or frameworks they work with.
                  Example: "Specializes in building scalable systems using Java, Spring Boot, React, and cloud-native architectures."
                - Third sentence: describe the type of impact or work style.
                  Example: "Proven track record of leading feature development and maintaining high code quality across collaborative teams."
                - Optional fourth sentence: current focus or forward-looking statement.
                  Example: "Currently focused on leveraging AI/ML technologies to enhance developer productivity tools."

                TONE:
                - Confident and factual, not boastful.
                - Reads like a senior engineer's LinkedIn summary.
                - NO buzzwords: never use "passionate", "ninja", "guru", "rockstar", "self-motivated", "hardworking".
                - NO fabricated metrics, numbers, or years of experience not in the source data.
                - NO markdown, bullet points, asterisks, dashes, or formatting.
                - Output ONLY the summary text as a plain paragraph. Nothing else.
                """;

        String truncatedReadme = rawReadme != null && rawReadme.length() > 2000
                ? rawReadme.substring(0, 2000) + "..." : (rawReadme != null ? rawReadme : "");

        String user = String.format(
                "Developer name: %s\nTop programming languages: %s\nGitHub profile data:\n%s\n\n"
                + "Write the professional summary now.",
                name,
                String.join(", ", topLanguages),
                truncatedReadme
        );

        String result = ollama.chat(system, user, 350);
        if (result != null) {
            result = result.replaceAll("(?i)^" + java.util.regex.Pattern.quote(name) + "\\s+(is|has)\\s+", "");
            if (result.startsWith("\"") && result.endsWith("\""))
                result = result.substring(1, result.length() - 1).trim();
            log.info("AI summary: {} chars for {}", result.length(), name);
        }
        return result;
    }

    // ─── Project description ─────────────────────────────────────────────────

    /**
     * Generate Enhancv-style project description as bullet points.
     * Each bullet starts with action verb and mentions technology + impact.
     *
     * Output format: multiple sentences separated by newlines, each a complete bullet.
     */
    public String enhanceProjectDescription(String projectName,
                                             String rawDescription,
                                             List<String> languages,
                                             int stars) {
        String system = """
                You are a professional resume writer who writes for Enhancv, a premium resume platform.
                Convert a GitHub project into professional resume bullet points.

                STYLE REQUIREMENTS (match Enhancv exactly):
                Write 3-5 bullet points about this project. Each bullet point should be on a separate line.

                BULLET POINT FORMAT:
                - Each bullet starts with a strong past-tense ACTION VERB:
                  Developed, Engineered, Built, Architected, Designed, Implemented, Created,
                  Integrated, Automated, Optimized, Deployed, Configured, Established, Maintained
                - Each bullet is ONE sentence, 15-25 words.
                - Mention specific technologies/languages naturally within the bullet.
                - When possible, include a measurable result or scope:
                  "...reducing load time by 40%", "...handling 10K+ requests per second",
                  "...used by 500+ developers", "...garnering N stars on GitHub"
                - For open-source projects with stars, include the star count in one bullet.

                EXAMPLE OUTPUT (3 bullets for a chat application):
                Engineered a real-time chat engine using WebSocket connections and Node.js, supporting concurrent messaging across multiple channels.
                Implemented end-to-end encryption and user authentication using JWT tokens, ensuring secure communication for all participants.
                Deployed the application on AWS with Docker containerization, achieving 99.9% uptime and garnering 150 stars on GitHub.

                RULES:
                - Do NOT start any bullet with the project name.
                - Do NOT use bullet markers (-, *, •) — just write each sentence on its own line.
                - Do NOT add numbering.
                - Do NOT use markdown or any formatting.
                - Do NOT use first person (I, my, we).
                - Be specific. Do NOT be vague or generic.
                - If you don't have enough info, make reasonable inferences from the project name and languages.
                - Output ONLY the bullet point sentences, one per line. Nothing else.
                """;

        String desc = rawDescription != null && !rawDescription.isBlank()
                ? rawDescription : "No description provided";

        String user = String.format(
                "Project name: %s\nProgramming languages: %s\nGitHub stars: %d\nOriginal description: %s\n\n"
                + "Write the resume bullet points now.",
                projectName,
                languages.isEmpty() ? "unknown" : String.join(", ", languages),
                stars,
                desc
        );

        String result = ollama.chat(system, user, 400);
        if (result != null) {
            // Clean up: remove any bullet markers, numbering, or markdown
            result = result.lines()
                    .map(line -> line.replaceAll("^\\d+[.)\\s]+", ""))  // remove numbering
                    .map(line -> line.replaceAll("^[-•*>]+\\s*", ""))   // remove bullet markers
                    .map(line -> line.replaceAll("\\*+", ""))           // remove asterisks
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .collect(java.util.stream.Collectors.joining("\n"));
            // Remove surrounding quotes
            if (result.startsWith("\"") && result.endsWith("\""))
                result = result.substring(1, result.length() - 1).trim();
            log.debug("AI project desc: {} lines for {}", result.split("\n").length, projectName);
        }
        return result;
    }
}
