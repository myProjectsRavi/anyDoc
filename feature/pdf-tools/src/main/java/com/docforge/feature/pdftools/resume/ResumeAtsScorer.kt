package com.docforge.feature.pdftools.resume

/**
 * Offline ATS (Applicant Tracking System) resume score checker.
 * Performs keyword matching between a resume text and a target job description.
 *
 * Sprint 5 feature — Resume ATS Score Checker.
 */
class ResumeAtsScorer {

    data class AtsScoreResult(
        val overallScore: Int,
        val matchedKeywords: List<String>,
        val missingKeywords: List<String>,
        val suggestions: List<String>,
        val sectionScores: Map<String, Int>
    )

    /**
     * Scores [resumeText] against [jobDescription].
     *
     * @return [AtsScoreResult] with 0–100 score and keyword analysis.
     */
    fun score(resumeText: String, jobDescription: String): AtsScoreResult {
        require(resumeText.isNotBlank()) { "Resume text is empty." }
        require(jobDescription.isNotBlank()) { "Job description is empty." }

        val resumeTokens = tokenize(resumeText)
        val jobTokens = tokenize(jobDescription)
        val jobKeywords = extractKeywords(jobTokens)
        val resumeLower = resumeText.lowercase()

        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()

        jobKeywords.forEach { keyword ->
            if (resumeLower.contains(keyword.lowercase())) {
                matched += keyword
            } else {
                missing += keyword
            }
        }

        val keywordScore = if (jobKeywords.isNotEmpty()) {
            (matched.size.toFloat() / jobKeywords.size * 100).toInt().coerceIn(0, 100)
        } else 0

        val sectionScores = mutableMapOf<String, Int>()
        sectionScores["Keywords"] = keywordScore
        sectionScores["Contact Info"] = scoreContactInfo(resumeText)
        sectionScores["Experience"] = scoreSectionPresence(resumeText, EXPERIENCE_HEADERS)
        sectionScores["Education"] = scoreSectionPresence(resumeText, EDUCATION_HEADERS)
        sectionScores["Skills"] = scoreSectionPresence(resumeText, SKILLS_HEADERS)

        val overall = sectionScores.values.average().toInt().coerceIn(0, 100)

        val suggestions = mutableListOf<String>()
        if (missing.isNotEmpty()) {
            suggestions += "Add these missing keywords: ${missing.take(10).joinToString(", ")}"
        }
        if (sectionScores["Contact Info"]!! < 50) {
            suggestions += "Include email and phone number in your resume."
        }
        if (sectionScores["Experience"]!! < 50) {
            suggestions += "Add a clear 'Experience' or 'Work History' section."
        }
        if (sectionScores["Skills"]!! < 50) {
            suggestions += "Add a dedicated 'Skills' section listing relevant technical skills."
        }
        if (resumeText.length < 300) {
            suggestions += "Resume appears too short. Aim for at least 300–600 words."
        }

        return AtsScoreResult(
            overallScore = overall,
            matchedKeywords = matched,
            missingKeywords = missing,
            suggestions = suggestions,
            sectionScores = sectionScores
        )
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9+#./ -]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
    }

    private fun extractKeywords(tokens: List<String>): List<String> {
        val stopWords = setOf(
            "the", "and", "for", "with", "that", "this", "are", "was", "will",
            "you", "your", "our", "their", "from", "have", "has", "had",
            "been", "being", "not", "but", "all", "can", "may", "must",
            "should", "would", "could", "about", "into", "over", "such"
        )
        val freq = tokens
            .filter { it !in stopWords && it.length > 2 }
            .groupingBy { it }
            .eachCount()

        // Return top keywords by frequency (≥2 occurrences preferred)
        return freq.entries
            .sortedByDescending { it.value }
            .take(50)
            .map { it.key }
    }

    private fun scoreContactInfo(text: String): Int {
        var score = 0
        if (text.contains(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))) score += 40
        if (text.contains(Regex("\\+?\\d[\\d\\s()-]{7,}"))) score += 30
        if (text.contains(Regex("linkedin\\.com|github\\.com", RegexOption.IGNORE_CASE))) score += 30
        return score.coerceIn(0, 100)
    }

    private fun scoreSectionPresence(text: String, headers: List<String>): Int {
        val lower = text.lowercase()
        return if (headers.any { lower.contains(it) }) 100 else 0
    }

    companion object {
        private val EXPERIENCE_HEADERS = listOf(
            "experience", "work history", "employment", "professional background",
            "career history", "work experience"
        )
        private val EDUCATION_HEADERS = listOf(
            "education", "academic", "qualifications", "degree", "university",
            "college", "school"
        )
        private val SKILLS_HEADERS = listOf(
            "skills", "technical skills", "core competencies", "technologies",
            "proficiencies", "expertise"
        )
    }
}
