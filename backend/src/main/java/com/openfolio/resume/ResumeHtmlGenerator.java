package com.openfolio.resume;

import com.openfolio.education.Education;
import com.openfolio.experience.Experience;
import com.openfolio.project.Project;
import com.openfolio.skill.Skill;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates professional resume HTML in 4 templates —
 * classic, modern, minimal, bold — for both WebView preview and PDF export.
 *
 * PDF output is XHTML-strict, CSS2-only (no flex/grid) using tables for layout.
 * Preview output uses modern CSS (flex, grid, etc.) for a richer experience.
 */
@Component
public class ResumeHtmlGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    // ─── Entry points ─────────────────────────────────────────────────────────

    /** Preview HTML (WebView — modern CSS). */
    public String generate(ResumeBundle b, String templateKey) {
        return switch (templateKey != null ? templateKey.toLowerCase() : "classic") {
            case "modern"  -> buildPreviewModern(b);
            case "minimal" -> buildPreviewMinimal(b);
            case "bold"    -> buildPreviewBold(b);
            default        -> buildPreviewClassic(b);
        };
    }

    /** PDF HTML (openhtmltopdf — XHTML, CSS2, tables). */
    public String generateForPdf(ResumeBundle b, String templateKey) {
        return switch (templateKey != null ? templateKey.toLowerCase() : "classic") {
            case "modern"  -> buildPdfModern(b);
            case "minimal" -> buildPdfMinimal(b);
            case "bold"    -> buildPdfBold(b);
            default        -> buildPdfClassic(b);
        };
    }

    // =========================================================================
    //  DATA HELPERS
    // =========================================================================

    private String name(ResumeBundle b) {
        String n = b.resume().getFullName();
        if (n != null && !n.isBlank()) return n;
        if (b.user() != null && b.user().getDisplayName() != null) return b.user().getDisplayName();
        return "Developer";
    }

    private String jobTitle(ResumeBundle b) {
        String t = b.resume().getJobTitle();
        return (t != null && !t.isBlank()) ? t : "";
    }

    private String email(ResumeBundle b) {
        String e = b.resume().getEmail();
        if (e != null && !e.isBlank()) return e;
        return (b.user() != null && b.user().getEmail() != null) ? b.user().getEmail() : "";
    }

    private String phone(ResumeBundle b) {
        String p = b.resume().getPhone();
        return (p != null && !p.isBlank()) ? p : "";
    }

    private String location(ResumeBundle b) {
        String l = b.resume().getLocation();
        return (l != null && !l.isBlank()) ? l : "";
    }

    private String website(ResumeBundle b) {
        return b.resume().getWebsite() != null ? b.resume().getWebsite() : "";
    }

    private String github(ResumeBundle b) {
        String g = b.resume().getGithubUrl();
        if (g != null && !g.isBlank()) return g;
        if (b.user() != null && b.user().getGithubUsername() != null)
            return "github.com/" + b.user().getGithubUsername();
        return "";
    }

    private String linkedin(ResumeBundle b) {
        return b.resume().getLinkedinUrl() != null ? b.resume().getLinkedinUrl() : "";
    }

    private String summary(ResumeBundle b) {
        return (b.aboutContent() != null && !b.aboutContent().isBlank()) ? b.aboutContent() : "";
    }

    private List<String> contactLines(ResumeBundle b) {
        List<String> lines = new ArrayList<>();
        String e = email(b); if (!e.isBlank()) lines.add(e);
        String p = phone(b); if (!p.isBlank()) lines.add(p);
        String l = location(b); if (!l.isBlank()) lines.add(l);
        String w = website(b); if (!w.isBlank()) lines.add(w);
        String g = github(b); if (!g.isBlank()) lines.add(g);
        String li = linkedin(b); if (!li.isBlank()) lines.add(li);
        return lines;
    }

    private String initials(String name) {
        return Arrays.stream(name.split("\\s+"))
                .limit(2).filter(w -> !w.isBlank())
                .map(w -> String.valueOf(Character.toUpperCase(w.charAt(0))))
                .collect(Collectors.joining());
    }

    // ── Skill grouping ─────────────────────────────────────────────────────
    private Map<String, List<Skill>> groupSkills(List<Skill> skills) {
        Map<String, List<Skill>> map = new LinkedHashMap<>();
        for (Skill s : skills) {
            String prof = s.getProficiency() != null ? capitalize(s.getProficiency().name()) : "Other";
            map.computeIfAbsent(prof, k -> new ArrayList<>()).add(s);
        }
        return map;
    }

    // =========================================================================
    //  1. CLASSIC TEMPLATE — Preview
    // =========================================================================

    private String buildPreviewClassic(ResumeBundle b) {
        StringBuilder sb = new StringBuilder();
        sb.append(previewHead(name(b), classicPreviewCss()));

        // Header
        sb.append("<div class=\"header\">\n");
        sb.append("<h1 class=\"name\">").append(esc(name(b))).append("</h1>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"job-title\">").append(esc(jt)).append("</div>\n");
        sb.append("<div class=\"contacts\">");
        for (String c : contactLines(b)) sb.append("<span class=\"contact\">").append(esc(c)).append("</span>");
        sb.append("</div>\n</div>\n");

        // Summary
        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append(previewSec("PROFESSIONAL SUMMARY", "classic"));
            sb.append("<p class=\"summary\">").append(esc(summ)).append("</p>\n");
        }

        // Skills
        if (!b.skills().isEmpty()) {
            sb.append(previewSec("TECHNICAL SKILLS", "classic"));
            sb.append(previewSkillsClassic(b.skills()));
        }

        // Experience
        if (!b.experiences().isEmpty()) {
            sb.append(previewSec("WORK EXPERIENCE", "classic"));
            sb.append(previewExperience(b.experiences(), "#7C3AED"));
        }

        // Projects
        if (!b.projects().isEmpty()) {
            sb.append(previewSec("NOTABLE PROJECTS", "classic"));
            sb.append(previewProjects(b.projects()));
        }

        // Education
        if (!b.educations().isEmpty()) {
            sb.append(previewSec("EDUCATION", "classic"));
            sb.append(previewEducation(b.educations()));
        }

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // =========================================================================
    //  2. MODERN TEMPLATE — Preview
    // =========================================================================

    private String buildPreviewModern(ResumeBundle b) {
        StringBuilder sb = new StringBuilder();
        sb.append(previewHead(name(b), modernPreviewCss()));

        // Header with colored bar
        sb.append("<div class=\"header-modern\">\n");
        sb.append("<div class=\"header-bar\"></div>\n");
        sb.append("<div class=\"header-content\">\n");
        sb.append("<div class=\"avatar-circle\">").append(esc(initials(name(b)))).append("</div>\n");
        sb.append("<div class=\"header-text\">\n");
        sb.append("<h1 class=\"name\">").append(esc(name(b))).append("</h1>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"job-title\">").append(esc(jt)).append("</div>\n");
        sb.append("</div>\n</div>\n");
        sb.append("<div class=\"contacts\">");
        for (String c : contactLines(b)) sb.append("<span class=\"contact\">").append(esc(c)).append("</span>");
        sb.append("</div>\n</div>\n");

        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append(previewSec("About Me", "modern"));
            sb.append("<p class=\"summary\">").append(esc(summ)).append("</p>\n");
        }

        if (!b.skills().isEmpty()) {
            sb.append(previewSec("Skills", "modern"));
            sb.append(previewSkillsModern(b.skills()));
        }

        if (!b.experiences().isEmpty()) {
            sb.append(previewSec("Experience", "modern"));
            sb.append(previewExperience(b.experiences(), "#2563EB"));
        }

        if (!b.projects().isEmpty()) {
            sb.append(previewSec("Projects", "modern"));
            sb.append(previewProjects(b.projects()));
        }

        if (!b.educations().isEmpty()) {
            sb.append(previewSec("Education", "modern"));
            sb.append(previewEducation(b.educations()));
        }

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // =========================================================================
    //  3. MINIMAL TEMPLATE — Preview
    // =========================================================================

    private String buildPreviewMinimal(ResumeBundle b) {
        StringBuilder sb = new StringBuilder();
        sb.append(previewHead(name(b), minimalPreviewCss()));

        sb.append("<div class=\"header-minimal\">\n");
        sb.append("<h1 class=\"name\">").append(esc(name(b))).append("</h1>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"job-title\">").append(esc(jt)).append("</div>\n");
        sb.append("<div class=\"contacts\">");
        for (String c : contactLines(b)) sb.append("<span class=\"contact\">").append(esc(c)).append("</span>");
        sb.append("</div>\n</div>\n");
        sb.append("<hr class=\"divider\"/>\n");

        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append(previewSec("Summary", "minimal"));
            sb.append("<p class=\"summary\">").append(esc(summ)).append("</p>\n");
        }

        if (!b.skills().isEmpty()) {
            sb.append(previewSec("Skills", "minimal"));
            sb.append(previewSkillsMinimal(b.skills()));
        }

        if (!b.experiences().isEmpty()) {
            sb.append(previewSec("Experience", "minimal"));
            sb.append(previewExperience(b.experiences(), "#0D9488"));
        }

        if (!b.projects().isEmpty()) {
            sb.append(previewSec("Projects", "minimal"));
            sb.append(previewProjects(b.projects()));
        }

        if (!b.educations().isEmpty()) {
            sb.append(previewSec("Education", "minimal"));
            sb.append(previewEducation(b.educations()));
        }

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // =========================================================================
    //  4. BOLD TEMPLATE — Preview
    // =========================================================================

    private String buildPreviewBold(ResumeBundle b) {
        StringBuilder sb = new StringBuilder();
        sb.append(previewHead(name(b), boldPreviewCss()));

        sb.append("<div class=\"header-bold\">\n");
        sb.append("<div class=\"header-bg\"></div>\n");
        sb.append("<div class=\"header-fg\">\n");
        sb.append("<h1 class=\"name\">").append(esc(name(b))).append("</h1>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"job-title\">").append(esc(jt)).append("</div>\n");
        sb.append("<div class=\"contacts\">");
        for (String c : contactLines(b)) sb.append("<span class=\"contact\">").append(esc(c)).append("</span>");
        sb.append("</div>\n</div>\n</div>\n");

        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append(previewSec("PROFILE", "bold"));
            sb.append("<p class=\"summary\">").append(esc(summ)).append("</p>\n");
        }

        if (!b.skills().isEmpty()) {
            sb.append(previewSec("SKILLS", "bold"));
            sb.append(previewSkillsBold(b.skills()));
        }

        if (!b.experiences().isEmpty()) {
            sb.append(previewSec("EXPERIENCE", "bold"));
            sb.append(previewExperience(b.experiences(), "#DC2626"));
        }

        if (!b.projects().isEmpty()) {
            sb.append(previewSec("PROJECTS", "bold"));
            sb.append(previewProjects(b.projects()));
        }

        if (!b.educations().isEmpty()) {
            sb.append(previewSec("EDUCATION", "bold"));
            sb.append(previewEducation(b.educations()));
        }

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // =========================================================================
    //  SHARED PREVIEW SECTION BUILDERS
    // =========================================================================

    private String previewHead(String title, String css) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
             + "<meta charset=\"UTF-8\">\n"
             + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
             + "<title>" + esc(title) + " — Resume</title>\n"
             + "<style>" + css + "</style>\n"
             + "</head>\n<body>\n<div class=\"page\">\n";
    }

    private String previewSec(String title, String template) {
        return "<div class=\"section\"><h2 class=\"sec-title\">" + esc(title) + "</h2>\n";
    }

    private String previewSkillsClassic(List<Skill> skills) {
        Map<String, List<Skill>> grouped = groupSkills(skills);
        StringBuilder sb = new StringBuilder("<div class=\"skills-grid\">\n");
        for (var entry : grouped.entrySet()) {
            sb.append("<div class=\"skill-row\">");
            sb.append("<span class=\"skill-level\">").append(esc(entry.getKey())).append("</span>");
            sb.append("<span class=\"skill-names\">");
            sb.append(entry.getValue().stream().map(s -> esc(s.getName())).collect(Collectors.joining("  ·  ")));
            sb.append("</span></div>\n");
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    private String previewSkillsModern(List<Skill> skills) {
        StringBuilder sb = new StringBuilder("<div class=\"skill-chips\">\n");
        for (Skill s : skills) {
            String level = s.getProficiency() != null ? s.getProficiency().name().toLowerCase() : "intermediate";
            sb.append("<span class=\"chip chip-").append(level).append("\">").append(esc(s.getName())).append("</span>");
        }
        sb.append("\n</div></div>\n");
        return sb.toString();
    }

    private String previewSkillsMinimal(List<Skill> skills) {
        StringBuilder sb = new StringBuilder("<div class=\"skill-list\">");
        sb.append(skills.stream().map(s -> esc(s.getName())).collect(Collectors.joining("  ·  ")));
        sb.append("</div></div>\n");
        return sb.toString();
    }

    private String previewSkillsBold(List<Skill> skills) {
        StringBuilder sb = new StringBuilder("<div class=\"skill-tags\">\n");
        for (Skill s : skills) {
            sb.append("<span class=\"tag\">").append(esc(s.getName())).append("</span>");
        }
        sb.append("\n</div></div>\n");
        return sb.toString();
    }

    private String previewExperience(List<Experience> exps, String accent) {
        StringBuilder sb = new StringBuilder("<div class=\"exp-list\">\n");
        for (Experience e : exps) {
            sb.append("<div class=\"exp-item\">\n");
            sb.append("<div class=\"exp-top\">\n");
            sb.append("<div class=\"exp-left\">\n");
            sb.append("<div class=\"exp-role\">").append(esc(e.getTitle())).append("</div>\n");
            sb.append("<div class=\"exp-company\">").append(esc(e.getCompany())).append("</div>\n");
            sb.append("</div>\n");
            String dates = formatDates(e);
            if (!dates.isBlank()) sb.append("<div class=\"exp-dates\">").append(esc(dates)).append("</div>\n");
            sb.append("</div>\n");
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("<p class=\"exp-desc\">").append(esc(e.getDescription())).append("</p>\n");
            }
            sb.append("</div>\n");
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    private String previewProjects(List<Project> projects) {
        List<Project> show = projects.stream().limit(6).toList();
        StringBuilder sb = new StringBuilder("<div class=\"proj-list\">\n");
        for (Project p : show) {
            sb.append("<div class=\"proj-item\">\n");
            sb.append("<div class=\"proj-top\">\n");
            sb.append("<span class=\"proj-name\">").append(esc(p.getName())).append("</span>\n");
            if (p.getStars() > 0) sb.append("<span class=\"proj-stars\">⭐ ").append(p.getStars()).append("</span>\n");
            sb.append("</div>\n");
            if (p.getLanguages() != null && !p.getLanguages().isEmpty()) {
                sb.append("<div class=\"proj-langs\">");
                for (String lang : p.getLanguages()) sb.append("<span class=\"lang-pill\">").append(esc(lang)).append("</span>");
                sb.append("</div>\n");
            }
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append("<p class=\"proj-desc\">").append(esc(p.getDescription())).append("</p>\n");
            }
            sb.append("</div>\n");
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    private String previewEducation(List<Education> edus) {
        StringBuilder sb = new StringBuilder("<div class=\"edu-list\">\n");
        for (Education e : edus) {
            sb.append("<div class=\"edu-item\">\n");
            String deg = ((e.getDegree() != null ? e.getDegree() : "") +
                         (e.getField() != null ? " in " + e.getField() : "")).trim();
            if (!deg.isBlank()) sb.append("<div class=\"edu-degree\">").append(esc(deg)).append("</div>\n");
            sb.append("<div class=\"edu-inst\">").append(esc(e.getInstitution())).append("</div>\n");
            String yrs = formatEduYears(e);
            if (!yrs.isBlank()) sb.append("<div class=\"edu-dates\">").append(esc(yrs)).append("</div>\n");
            sb.append("</div>\n");
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    // =========================================================================
    //  PDF BUILDERS — 4 DISTINCT LAYOUTS (XHTML, CSS2, tables)
    // =========================================================================

    private String pdfDocStart(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
             + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
             + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
             + "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n<head>\n"
             + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
             + "<title>" + esc(title) + " — Resume</title>\n";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. CLASSIC PDF — Clean centered header, accent underline, grouped skills
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPdfClassic(ResumeBundle b) {
        String A = "#7C3AED", T = "#111827", S = "#374151", M = "#6B7280", BG = "#F5F3FF", BD = "#E5E7EB";
        StringBuilder sb = new StringBuilder();
        sb.append(pdfDocStart(name(b)));
        sb.append("<style>\n");
        sb.append("body{margin:30px 44px;font-family:'Helvetica Neue',Arial,sans-serif;font-size:10.5pt;color:").append(T).append(";line-height:1.5}");
        // Centered header
        sb.append(".hdr{text-align:center;padding-bottom:14px;border-bottom:2.5px solid ").append(A).append(";margin-bottom:18px}");
        sb.append(".hdr-name{font-size:24pt;font-weight:bold;color:").append(T).append(";letter-spacing:-0.5px;line-height:1.15}");
        sb.append(".hdr-title{font-size:11pt;color:").append(A).append(";margin-top:4px;font-weight:600}");
        sb.append(".hdr-contacts{font-size:8.5pt;color:").append(M).append(";margin-top:6px}");
        sb.append(".hdr-sep{color:").append(BD).append("}");
        // Sections
        sb.append(".sec{border-bottom:1px solid ").append(BD).append(";margin:18px 0 10px;padding-bottom:4px}");
        sb.append(".sec-t{font-size:8pt;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:").append(A).append("}");
        sb.append(".sum{font-size:10.5pt;color:").append(S).append(";line-height:1.7;margin-bottom:6px}");
        // Skills grid table
        sb.append(".sk-tbl{margin-bottom:6px}");
        sb.append(".sk-lvl{width:92pt;font-size:8.5pt;font-weight:bold;color:").append(M).append(";text-transform:uppercase;letter-spacing:0.8px;padding:3px 0;vertical-align:top}");
        sb.append(".sk-val{font-size:10pt;color:").append(S).append(";padding:3px 0;vertical-align:top}");
        // Blocks
        sb.append(".blk{margin-bottom:12px;page-break-inside:avoid}");
        sb.append(".role{font-size:10.5pt;font-weight:bold;color:").append(T).append("}");
        sb.append(".co{font-weight:normal;color:").append(A).append("}");
        sb.append(".dt{font-size:8.5pt;color:").append(M).append(";text-align:right;white-space:nowrap;vertical-align:middle}");
        sb.append(".desc{font-size:9.5pt;color:").append(S).append(";margin:3px 0 0;line-height:1.6}");
        sb.append(".pj-lang{font-size:9pt;color:").append(M).append(";font-weight:normal}");
        sb.append(".edu-inst{font-size:9.5pt;color:").append(A).append(";margin-top:2px}");
        sb.append("\n</style>\n</head>\n<body>\n");

        // Header — centered
        sb.append("<div class=\"hdr\">\n");
        sb.append("<div class=\"hdr-name\">").append(esc(name(b))).append("</div>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"hdr-title\">").append(esc(jt)).append("</div>\n");
        List<String> cl = contactLines(b);
        if (!cl.isEmpty()) {
            sb.append("<div class=\"hdr-contacts\">");
            sb.append(cl.stream().map(this::esc).collect(Collectors.joining("  <span class=\"hdr-sep\">|</span>  ")));
            sb.append("</div>\n");
        }
        sb.append("</div>\n");

        appendPdfBody(sb, b, A, M, "classic");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. MODERN PDF — Two-column header (initials circle + name), colored skill badges
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPdfModern(ResumeBundle b) {
        String A = "#2563EB", A2 = "#7C3AED", T = "#1E293B", S = "#475569", M = "#64748B", BD = "#E2E8F0";
        StringBuilder sb = new StringBuilder();
        sb.append(pdfDocStart(name(b)));
        sb.append("<style>\n");
        sb.append("body{margin:0;font-family:'Helvetica Neue',Arial,sans-serif;font-size:10.5pt;color:").append(T).append(";line-height:1.5}");
        // Full-width gradient header bar
        sb.append(".hdr-bar{background-color:").append(A).append(";height:4px;margin:0}");
        sb.append(".hdr{padding:22px 44px 16px;margin-bottom:4px}");
        sb.append(".hdr-inner td{vertical-align:middle}");
        sb.append(".avatar{width:50pt;height:50pt;background-color:").append(A).append(";color:#FFFFFF;font-size:18pt;font-weight:bold;text-align:center;line-height:50pt;-webkit-border-radius:25pt}");
        sb.append(".hdr-name{font-size:22pt;font-weight:bold;color:").append(T).append(";letter-spacing:-0.3px}");
        sb.append(".hdr-title{font-size:10.5pt;color:").append(A).append(";font-weight:600;margin-top:2px}");
        sb.append(".hdr-contacts{font-size:8.5pt;color:").append(M).append(";margin-top:6px}");
        sb.append(".c-pill{background-color:#F1F5F9;padding:2px 6px;font-size:8pt;color:").append(M).append("}");
        // Content area
        sb.append(".content{padding:0 44px 28px}");
        sb.append(".sec{border-bottom:1px solid ").append(BD).append(";margin:16px 0 10px;padding-bottom:4px}");
        sb.append(".sec-t{font-size:8pt;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:").append(A).append("}");
        sb.append(".sum{font-size:10.5pt;color:").append(S).append(";line-height:1.7;margin-bottom:6px}");
        // Skill chips as inline boxes
        sb.append(".chip{display:inline;font-size:9pt;font-weight:600;padding:3px 8px;margin-right:4px;margin-bottom:4px;border:1px solid ").append(BD).append(";background-color:#F8FAFC;color:").append(S).append("}");
        // Blocks
        sb.append(".blk{margin-bottom:12px;page-break-inside:avoid}");
        sb.append(".role{font-size:10.5pt;font-weight:bold;color:").append(T).append("}");
        sb.append(".co{font-weight:normal;color:").append(A).append("}");
        sb.append(".dt{font-size:8.5pt;color:").append(M).append(";text-align:right;white-space:nowrap;vertical-align:middle}");
        sb.append(".desc{font-size:9.5pt;color:").append(S).append(";margin:3px 0 0;line-height:1.6}");
        sb.append(".pj-lang{font-size:9pt;color:").append(M).append(";font-weight:normal}");
        sb.append(".edu-inst{font-size:9.5pt;color:").append(A).append(";margin-top:2px}");
        sb.append("\n</style>\n</head>\n<body>\n");

        // Gradient bar
        sb.append("<div class=\"hdr-bar\"> </div>\n");
        // Header
        sb.append("<div class=\"hdr\">\n");
        sb.append("<table class=\"hdr-inner\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
        sb.append("<td style=\"width:60pt;vertical-align:middle\">");
        sb.append("<div class=\"avatar\">").append(esc(initials(name(b)))).append("</div>");
        sb.append("</td>\n<td style=\"padding-left:12px\">\n");
        sb.append("<div class=\"hdr-name\">").append(esc(name(b))).append("</div>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"hdr-title\">").append(esc(jt)).append("</div>\n");
        sb.append("</td>\n</tr></table>\n");
        List<String> cl = contactLines(b);
        if (!cl.isEmpty()) {
            sb.append("<div class=\"hdr-contacts\" style=\"margin-top:10px\">");
            for (String c : cl) sb.append("<span class=\"c-pill\">").append(esc(c)).append("</span>  ");
            sb.append("</div>\n");
        }
        sb.append("</div>\n");

        sb.append("<div class=\"content\">\n");
        appendPdfBody(sb, b, A, M, "modern");
        sb.append("</div>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. MINIMAL PDF — Left-aligned, airy spacing, thin hairline dividers, inline skills
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPdfMinimal(ResumeBundle b) {
        String A = "#0D9488", T = "#111827", S = "#374151", M = "#6B7280", BD = "#E5E7EB";
        StringBuilder sb = new StringBuilder();
        sb.append(pdfDocStart(name(b)));
        sb.append("<style>\n");
        sb.append("body{margin:40px 50px;font-family:Georgia,'Times New Roman',serif;font-size:10.5pt;color:").append(T).append(";line-height:1.6}");
        // Minimal header — left-aligned, no border, large name
        sb.append(".hdr{margin-bottom:6px}");
        sb.append(".hdr-name{font-size:26pt;font-weight:normal;color:").append(T).append(";letter-spacing:-0.3px;line-height:1.15}");
        sb.append(".hdr-title{font-size:11pt;color:").append(A).append(";font-weight:normal;margin-top:3px;font-style:italic}");
        sb.append(".hdr-contacts{font-size:8.5pt;color:").append(M).append(";margin-top:8px}");
        sb.append(".hr{border:none;border-top:0.5px solid ").append(BD).append(";margin:14px 0}");
        // Sections — minimal
        sb.append(".sec{margin:16px 0 8px}");
        sb.append(".sec-t{font-size:9pt;font-weight:bold;letter-spacing:1.5px;text-transform:uppercase;color:").append(A).append("}");
        sb.append(".sum{font-size:10.5pt;color:").append(S).append(";line-height:1.8;margin-bottom:6px}");
        // Skills as simple comma-separated text
        sb.append(".sk-text{font-size:10pt;color:").append(S).append(";line-height:1.8}");
        // Blocks
        sb.append(".blk{margin-bottom:13px;page-break-inside:avoid}");
        sb.append(".role{font-size:10.5pt;font-weight:bold;color:").append(T).append("}");
        sb.append(".co{font-weight:normal;font-style:italic;color:").append(A).append("}");
        sb.append(".dt{font-size:8.5pt;color:").append(M).append(";text-align:right;white-space:nowrap;vertical-align:middle}");
        sb.append(".desc{font-size:9.5pt;color:").append(S).append(";margin:3px 0 0;line-height:1.7}");
        sb.append(".pj-lang{font-size:9pt;color:").append(M).append(";font-weight:normal;font-style:italic}");
        sb.append(".edu-inst{font-size:9.5pt;color:").append(A).append(";margin-top:2px;font-style:italic}");
        sb.append("\n</style>\n</head>\n<body>\n");

        // Header — left-aligned, elegant
        sb.append("<div class=\"hdr\">\n");
        sb.append("<div class=\"hdr-name\">").append(esc(name(b))).append("</div>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"hdr-title\">").append(esc(jt)).append("</div>\n");
        List<String> cl = contactLines(b);
        if (!cl.isEmpty()) {
            sb.append("<div class=\"hdr-contacts\">");
            sb.append(cl.stream().map(this::esc).collect(Collectors.joining("  ·  ")));
            sb.append("</div>\n");
        }
        sb.append("</div>\n");
        sb.append("<hr class=\"hr\"/>\n");

        // Summary
        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">Summary</div></div>\n");
            sb.append("<p class=\"sum\">").append(esc(summ)).append("</p>\n");
        }

        // Skills — simple inline text
        if (!b.skills().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">Skills</div></div>\n");
            sb.append("<p class=\"sk-text\">");
            sb.append(b.skills().stream().map(s -> esc(s.getName())).collect(Collectors.joining("  ·  ")));
            sb.append("</p>\n");
        }

        // Experience
        if (!b.experiences().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">Experience</div></div>\n");
            appendPdfExperience(sb, b.experiences());
        }

        // Projects
        if (!b.projects().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">Projects</div></div>\n");
            appendPdfProjects(sb, b.projects());
        }

        // Education
        if (!b.educations().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">Education</div></div>\n");
            appendPdfEducation(sb, b.educations());
        }

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. BOLD PDF — Dark header block, white text, red accents, high contrast
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPdfBold(ResumeBundle b) {
        String A = "#DC2626", T = "#111827", S = "#374151", M = "#6B7280", BD = "#E5E7EB";
        StringBuilder sb = new StringBuilder();
        sb.append(pdfDocStart(name(b)));
        sb.append("<style>\n");
        sb.append("body{margin:0;font-family:'Helvetica Neue',Arial,sans-serif;font-size:10.5pt;color:").append(T).append(";line-height:1.5}");
        // Dark header
        sb.append(".hdr{background-color:#1E1E2E;padding:28px 44px 22px;margin-bottom:4px}");
        sb.append(".hdr-name{font-size:26pt;font-weight:bold;color:#FFFFFF;letter-spacing:-0.5px;line-height:1.15}");
        sb.append(".hdr-title{font-size:11pt;color:").append(A).append(";margin-top:4px;font-weight:600}");
        sb.append(".hdr-contacts{font-size:8.5pt;color:rgba(255,255,255,0.65);margin-top:8px}");
        sb.append(".hdr-sep{color:rgba(255,255,255,0.25)}");
        // Content area
        sb.append(".content{padding:4px 44px 28px}");
        sb.append(".sec{border-bottom:2px solid ").append(A).append(";margin:16px 0 10px;padding-bottom:4px}");
        sb.append(".sec-t{font-size:8.5pt;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:").append(A).append("}");
        sb.append(".sum{font-size:10.5pt;color:").append(S).append(";line-height:1.7;margin-bottom:6px}");
        // Skill tags
        sb.append(".tag{display:inline;font-size:9pt;font-weight:bold;padding:3px 8px;margin-right:4px;background-color:#FEF2F2;color:").append(A).append(";border:1px solid #FECACA}");
        // Blocks
        sb.append(".blk{margin-bottom:12px;page-break-inside:avoid;padding-left:10px;border-left:3px solid ").append(A).append("}");
        sb.append(".role{font-size:10.5pt;font-weight:bold;color:").append(T).append("}");
        sb.append(".co{font-weight:600;color:").append(A).append("}");
        sb.append(".dt{font-size:8.5pt;color:").append(M).append(";text-align:right;white-space:nowrap;vertical-align:middle}");
        sb.append(".desc{font-size:9.5pt;color:").append(S).append(";margin:3px 0 0;line-height:1.6}");
        sb.append(".pj-lang{font-size:9pt;color:").append(M).append(";font-weight:normal}");
        sb.append(".edu-inst{font-size:9.5pt;color:").append(A).append(";margin-top:2px;font-weight:600}");
        sb.append("\n</style>\n</head>\n<body>\n");

        // Dark header
        sb.append("<div class=\"hdr\">\n");
        sb.append("<div class=\"hdr-name\">").append(esc(name(b))).append("</div>\n");
        String jt = jobTitle(b);
        if (!jt.isBlank()) sb.append("<div class=\"hdr-title\">").append(esc(jt)).append("</div>\n");
        List<String> cl = contactLines(b);
        if (!cl.isEmpty()) {
            sb.append("<div class=\"hdr-contacts\">");
            sb.append(cl.stream().map(this::esc).collect(Collectors.joining("  <span class=\"hdr-sep\">|</span>  ")));
            sb.append("</div>\n");
        }
        sb.append("</div>\n");

        sb.append("<div class=\"content\">\n");

        // Summary
        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">PROFILE</div></div>\n");
            sb.append("<p class=\"sum\">").append(esc(summ)).append("</p>\n");
        }

        // Skills — tag style
        if (!b.skills().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">SKILLS</div></div>\n");
            sb.append("<p>");
            for (Skill s : b.skills()) sb.append("<span class=\"tag\">").append(esc(s.getName())).append("</span> ");
            sb.append("</p>\n");
        }

        // Experience — with left border
        if (!b.experiences().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">EXPERIENCE</div></div>\n");
            appendPdfExperience(sb, b.experiences());
        }

        // Projects
        if (!b.projects().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">PROJECTS</div></div>\n");
            appendPdfProjects(sb, b.projects());
        }

        // Education
        if (!b.educations().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">EDUCATION</div></div>\n");
            appendPdfEducation(sb, b.educations());
        }

        sb.append("</div>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ─── Shared PDF section content helpers ───────────────────────────────────

    private void appendPdfBody(StringBuilder sb, ResumeBundle b, String accent, String muted, String template) {
        String summ = summary(b);
        if (!summ.isBlank()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">");
            sb.append("classic".equals(template) ? "PROFESSIONAL SUMMARY" : "About Me");
            sb.append("</div></div>\n");
            sb.append("<p class=\"sum\">").append(esc(summ)).append("</p>\n");
        }

        if (!b.skills().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">");
            sb.append("classic".equals(template) ? "TECHNICAL SKILLS" : "Skills");
            sb.append("</div></div>\n");
            if ("classic".equals(template)) {
                // Grouped table
                Map<String, List<Skill>> grouped = groupSkills(b.skills());
                sb.append("<table class=\"sk-tbl\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");
                for (String level : List.of("Expert", "Advanced", "Intermediate", "Beginner", "Other")) {
                    List<Skill> group = grouped.get(level);
                    if (group == null || group.isEmpty()) continue;
                    sb.append("<tr><td class=\"sk-lvl\">").append(esc(level)).append("</td>")
                      .append("<td class=\"sk-val\">").append(group.stream().map(s -> esc(s.getName())).collect(Collectors.joining("  ·  ")))
                      .append("</td></tr>\n");
                }
                sb.append("</table>\n");
            } else {
                // Chip/badge style
                sb.append("<p>");
                for (Skill s : b.skills()) sb.append("<span class=\"chip\">").append(esc(s.getName())).append("</span> ");
                sb.append("</p>\n");
            }
        }

        if (!b.experiences().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">");
            sb.append("classic".equals(template) ? "WORK EXPERIENCE" : "Experience");
            sb.append("</div></div>\n");
            appendPdfExperience(sb, b.experiences());
        }

        if (!b.projects().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">");
            sb.append("classic".equals(template) ? "NOTABLE PROJECTS" : "Projects");
            sb.append("</div></div>\n");
            appendPdfProjects(sb, b.projects());
        }

        if (!b.educations().isEmpty()) {
            sb.append("<div class=\"sec\"><div class=\"sec-t\">");
            sb.append("classic".equals(template) ? "EDUCATION" : "Education");
            sb.append("</div></div>\n");
            appendPdfEducation(sb, b.educations());
        }
    }

    private void appendPdfExperience(StringBuilder sb, List<Experience> exps) {
        for (Experience e : exps) {
            sb.append("<div class=\"blk\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
            sb.append("<td class=\"role\"><span>").append(esc(e.getTitle()))
              .append("</span>  —  <span class=\"co\">")
              .append(esc(e.getCompany())).append("</span></td>\n");
            sb.append("<td class=\"dt\">").append(esc(formatDates(e))).append("</td>\n");
            sb.append("</tr></table>\n");
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("<p class=\"desc\">").append(esc(e.getDescription())).append("</p>\n");
            }
            sb.append("</div>\n");
        }
    }

    private void appendPdfProjects(StringBuilder sb, List<Project> projects) {
        List<Project> show = projects.stream().limit(6).toList();
        for (Project p : show) {
            sb.append("<div class=\"blk\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
            sb.append("<td class=\"role\">").append(esc(p.getName()));
            if (p.getLanguages() != null && !p.getLanguages().isEmpty()) {
                sb.append("  <span class=\"pj-lang\">").append(esc(String.join(", ", p.getLanguages()))).append("</span>");
            }
            sb.append("</td>\n");
            if (p.getStars() > 0) sb.append("<td class=\"dt\">").append(p.getStars()).append(" ★</td>\n");
            else sb.append("<td class=\"dt\"></td>\n");
            sb.append("</tr></table>\n");
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append("<p class=\"desc\">").append(esc(p.getDescription())).append("</p>\n");
            }
            sb.append("</div>\n");
        }
    }

    private void appendPdfEducation(StringBuilder sb, List<Education> edus) {
        for (Education e : edus) {
            sb.append("<div class=\"blk\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
            String deg = ((e.getDegree() != null ? e.getDegree() : "") +
                         (e.getField() != null ? " in " + e.getField() : "")).trim();
            sb.append("<td class=\"role\">").append(esc(deg.isBlank() ? e.getInstitution() : deg)).append("</td>\n");
            sb.append("<td class=\"dt\">").append(esc(formatEduYears(e))).append("</td>\n");
            sb.append("</tr></table>\n");
            if (!deg.isBlank()) sb.append("<div class=\"edu-inst\">").append(esc(e.getInstitution())).append("</div>\n");
            sb.append("</div>\n");
        }
    }

    // =========================================================================
    //  PREVIEW CSS — each template
    // =========================================================================

    private String classicPreviewCss() {
        return baseCss()
             + "body{background:#FFFFFF;color:#111827}"
             + ".page{max-width:720px;margin:0 auto;padding:32px 28px}"
             + ".header{text-align:center;padding-bottom:20px;border-bottom:2px solid #7C3AED;margin-bottom:24px}"
             + ".name{font-size:32px;font-weight:800;color:#111827;letter-spacing:-0.5px;margin:0 0 4px}"
             + ".job-title{font-size:16px;color:#7C3AED;font-weight:600;margin-bottom:10px}"
             + ".contacts{display:flex;justify-content:center;flex-wrap:wrap;gap:14px}"
             + ".contact{font-size:12px;color:#6B7280}"
             + secCss("#7C3AED")
             + ".summary{font-size:14px;color:#374151;line-height:1.8}"
             + skillsCss()
             + expCss("#7C3AED")
             + projCss("#7C3AED", "#F5F3FF")
             + eduCss("#7C3AED");
    }

    private String modernPreviewCss() {
        return baseCss()
             + "body{background:#F8FAFC;color:#1E293B}"
             + ".page{max-width:720px;margin:0 auto;padding:0 28px 40px}"
             + ".header-modern{background:#FFFFFF;border-radius:0 0 20px 20px;padding:28px;margin-bottom:24px;box-shadow:0 2px 12px rgba(0,0,0,0.05)}"
             + ".header-bar{height:4px;background:linear-gradient(90deg,#2563EB,#7C3AED);border-radius:2px;margin-bottom:20px}"
             + ".header-content{display:flex;align-items:center;gap:16px;margin-bottom:14px}"
             + ".avatar-circle{width:56px;height:56px;border-radius:50%;background:linear-gradient(135deg,#2563EB,#7C3AED);display:flex;align-items:center;justify-content:center;color:#fff;font-size:20px;font-weight:800;flex-shrink:0}"
             + ".header-text{flex:1}"
             + ".name{font-size:28px;font-weight:800;color:#1E293B;letter-spacing:-0.3px;margin:0 0 2px}"
             + ".job-title{font-size:15px;color:#2563EB;font-weight:600}"
             + ".contacts{display:flex;flex-wrap:wrap;gap:12px}"
             + ".contact{font-size:12px;color:#64748B;background:#F1F5F9;padding:3px 10px;border-radius:20px}"
             + secCss("#2563EB")
             + ".summary{font-size:14px;color:#475569;line-height:1.8}"
             + ".skill-chips{display:flex;flex-wrap:wrap;gap:8px}"
             + ".chip{font-size:13px;font-weight:600;padding:6px 14px;border-radius:20px;border:1px solid}"
             + ".chip-expert{background:rgba(245,158,11,0.1);color:#D97706;border-color:rgba(245,158,11,0.3)}"
             + ".chip-advanced{background:rgba(139,92,246,0.1);color:#7C3AED;border-color:rgba(139,92,246,0.3)}"
             + ".chip-intermediate{background:rgba(16,185,129,0.1);color:#059669;border-color:rgba(16,185,129,0.3)}"
             + ".chip-beginner{background:rgba(59,130,246,0.1);color:#2563EB;border-color:rgba(59,130,246,0.3)}"
             + expCss("#2563EB")
             + projCss("#2563EB", "#EFF6FF")
             + eduCss("#2563EB");
    }

    private String minimalPreviewCss() {
        return baseCss()
             + "body{background:#FFFFFF;color:#111827}"
             + ".page{max-width:680px;margin:0 auto;padding:40px 28px}"
             + ".header-minimal{padding-bottom:16px}"
             + ".name{font-size:30px;font-weight:700;color:#111827;letter-spacing:-0.3px;margin:0 0 4px}"
             + ".job-title{font-size:15px;color:#0D9488;font-weight:500;margin-bottom:10px}"
             + ".contacts{display:flex;flex-wrap:wrap;gap:10px}"
             + ".contact{font-size:12px;color:#6B7280}"
             + ".divider{border:none;border-top:1px solid #E5E7EB;margin:0 0 24px}"
             + ".section{margin-bottom:20px}"
             + ".sec-title{font-size:12px;font-weight:700;letter-spacing:1.5px;color:#0D9488;text-transform:uppercase;margin:0 0 12px;border-bottom:none}"
             + ".summary{font-size:14px;color:#374151;line-height:1.8}"
             + ".skill-list{font-size:14px;color:#374151;line-height:2}"
             + expCss("#0D9488")
             + projCss("#0D9488", "#F0FDFA")
             + eduCss("#0D9488");
    }

    private String boldPreviewCss() {
        return baseCss()
             + "body{background:#FAFAFA;color:#111827}"
             + ".page{max-width:720px;margin:0 auto;padding:0 28px 40px}"
             + ".header-bold{position:relative;overflow:hidden;border-radius:0 0 20px 20px;margin-bottom:24px}"
             + ".header-bg{position:absolute;top:0;left:0;right:0;bottom:0;background:linear-gradient(135deg,#1E1E2E 0%,#2D1B3D 100%)}"
             + ".header-fg{position:relative;padding:32px 28px 24px;z-index:1}"
             + ".name{font-size:32px;font-weight:900;color:#FFFFFF;letter-spacing:-0.5px;margin:0 0 4px}"
             + ".job-title{font-size:16px;color:#F87171;font-weight:600;margin-bottom:12px}"
             + ".contacts{display:flex;flex-wrap:wrap;gap:12px}"
             + ".contact{font-size:12px;color:rgba(255,255,255,0.7)}"
             + secCss("#DC2626")
             + ".summary{font-size:14px;color:#374151;line-height:1.8}"
             + ".skill-tags{display:flex;flex-wrap:wrap;gap:8px}"
             + ".tag{font-size:13px;font-weight:600;padding:6px 14px;border-radius:8px;background:#FEF2F2;color:#DC2626;border:1px solid #FECACA}"
             + expCss("#DC2626")
             + projCss("#DC2626", "#FEF2F2")
             + eduCss("#DC2626");
    }

    // ── Shared CSS fragments ──────────────────────────────────────────────────

    private String baseCss() {
        return "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}"
             + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;font-size:15px;line-height:1.6}";
    }

    private String secCss(String accent) {
        return ".section{margin-bottom:22px}"
             + ".sec-title{font-size:11px;font-weight:800;letter-spacing:2px;text-transform:uppercase;color:" + accent + ";margin:0 0 14px;padding-bottom:6px;border-bottom:1px solid #E5E7EB}";
    }

    private String skillsCss() {
        return ".skills-grid{display:flex;flex-direction:column;gap:8px}"
             + ".skill-row{display:flex;gap:14px}"
             + ".skill-level{width:95px;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.5px;color:#9CA3AF;padding-top:2px;flex-shrink:0}"
             + ".skill-names{font-size:14px;color:#374151}";
    }

    private String expCss(String accent) {
        return ".exp-list{display:flex;flex-direction:column;gap:18px}"
             + ".exp-item{padding-left:14px;border-left:2px solid " + accent + "40}"
             + ".exp-top{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:4px}"
             + ".exp-left{flex:1}"
             + ".exp-role{font-size:15px;font-weight:700;color:#111827}"
             + ".exp-company{font-size:13px;color:" + accent + ";font-weight:500}"
             + ".exp-dates{font-size:12px;color:#9CA3AF;white-space:nowrap;margin-left:12px}"
             + ".exp-desc{font-size:13px;color:#4B5563;line-height:1.7}";
    }

    private String projCss(String accent, String bg) {
        return ".proj-list{display:flex;flex-direction:column;gap:12px}"
             + ".proj-item{background:" + bg + ";border:1px solid #E5E7EB;border-radius:10px;padding:14px 16px}"
             + ".proj-top{display:flex;justify-content:space-between;align-items:center;margin-bottom:6px}"
             + ".proj-name{font-size:15px;font-weight:700;color:#111827}"
             + ".proj-stars{font-size:12px;color:#9CA3AF}"
             + ".proj-langs{display:flex;flex-wrap:wrap;gap:5px;margin-bottom:6px}"
             + ".lang-pill{font-size:11px;font-weight:600;padding:2px 8px;border-radius:20px;background:" + accent + "15;color:" + accent + ";border:1px solid " + accent + "30}"
             + ".proj-desc{font-size:13px;color:#4B5563;line-height:1.6}";
    }

    private String eduCss(String accent) {
        return ".edu-list{display:flex;flex-direction:column;gap:10px}"
             + ".edu-item{padding:12px 14px;border-radius:8px;border:1px solid #E5E7EB}"
             + ".edu-degree{font-size:15px;font-weight:700;color:#111827}"
             + ".edu-inst{font-size:13px;color:" + accent + "}"
             + ".edu-dates{font-size:12px;color:#9CA3AF;margin-top:2px}";
    }

    // =========================================================================
    //  Utilities
    // =========================================================================

    private String formatDates(Experience e) {
        String start = e.getStartDate() != null ? e.getStartDate().format(DATE_FMT) : "";
        String end = e.isCurrent() ? "Present" : (e.getEndDate() != null ? e.getEndDate().format(DATE_FMT) : "");
        if (start.isBlank() && end.isBlank()) return "";
        return start + (end.isBlank() ? "" : " – " + end);
    }

    private String formatEduYears(Education e) {
        String start = e.getStartYear() != null ? String.valueOf(e.getStartYear()) : "";
        String end = e.getEndYear() != null ? String.valueOf(e.getEndYear()) : "";
        if (start.isBlank() && end.isBlank()) return "";
        return start + (end.isBlank() ? "" : " – " + end);
    }

    private String esc(Object o) {
        if (o == null) return "";
        return o.toString()
                .replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
