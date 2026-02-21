package com.openfolio.portfolio;

import com.openfolio.certification.Certification;
import com.openfolio.education.Education;
import com.openfolio.experience.Experience;
import com.openfolio.export.dto.ExportOptions;
import com.openfolio.project.Project;
import com.openfolio.skill.Skill;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates professional resume HTML modeled after Enhancv premium templates.
 *
 * <p>PDF templates use XHTML 1.0 Strict with CSS 2.1 (openhtmltopdf-compatible).
 * Layout follows Enhancv conventions:
 * <ul>
 *   <li>Dark navy header banner with uppercase name + pipe-separated tagline</li>
 *   <li>Contact info in a compact single row (phone | email | LinkedIn | GitHub)</li>
 *   <li>Two-column body: main (experience/projects) + sidebar (skills/education/stats)</li>
 *   <li>Bullet-point descriptions starting with action verbs</li>
 *   <li>Skills displayed as grouped tags</li>
 *   <li>Clean section dividers with uppercase labels</li>
 * </ul>
 */
@Component
public class PortfolioHtmlGenerator {

    // ─── Entry points ─────────────────────────────────────────────────────────

    public String generate(PortfolioBundle b) {
        String key = b.portfolio().getThemeKey();
        if (key == null) key = "dark";
        return buildPreview(b, key.toLowerCase());
    }

    public String generateForPdf(PortfolioBundle b) {
        return buildPdf(b, "pdf", ExportOptions.defaults());
    }

    public String generateForPdf(PortfolioBundle b, String templateKey) {
        return buildPdf(b, templateKey != null ? templateKey.toLowerCase() : "pdf", ExportOptions.defaults());
    }

    public String generateForPdf(PortfolioBundle b, String templateKey, ExportOptions options) {
        return buildPdf(b, templateKey != null ? templateKey.toLowerCase() : "pdf",
                options != null ? options : ExportOptions.defaults());
    }

    // =========================================================================
    //  PREVIEW  (WebView — full modern CSS with flex/grid)
    // =========================================================================

    private String buildPreview(PortfolioBundle b, String themeKey) {
        record T(String bg, String surface, String card, String text, String sub,
                 String muted, String primary, String pLight, String border, String font,
                 boolean light, boolean hacker) {}

        T t = switch (themeKey) {
            case "minimal" -> new T("#FAFAFA","#FFFFFF","#F5F3FF","#111827","#374151","#6B7280",
                    "#8B5CF6","#7C3AED","#E5E7EB",
                    "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif", true, false);
            case "hacker"  -> new T("#000000","#050505","#0A120A","#00FF41","#00CC33","#008F2C",
                    "#00FF41","#00CC33","rgba(0,255,65,0.18)",
                    "'Courier New','Lucida Console',monospace", false, true);
            default        -> new T("#09090B","#111113","#1C1C28","#F4F4F5","#A1A1AA","#71717A",
                    "#8B5CF6","#A78BFA","rgba(255,255,255,0.08)",
                    "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif", false, false);
        };

        String displayName = resolveDisplayName(b);
        String initials = initials(displayName);
        String email = b.user() != null ? b.user().getEmail() : "";
        String ghUser = b.user() != null ? b.user().getGithubUsername() : null;

        String nameStyle = t.hacker()
                ? "color:" + t.primary() + ";text-shadow:0 0 20px rgba(0,255,65,0.5)"
                : t.light() ? "color:" + t.text()
                : "background:linear-gradient(135deg,#C4B5FD 0%,#67E8F9 100%);"
                  + "-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
          .append("<title>").append(esc(displayName)).append("</title>\n")
          .append("<style>").append(previewCss(t.bg(), t.surface(), t.card(), t.text(),
                  t.sub(), t.muted(), t.primary(), t.pLight(), t.border(), t.font(),
                  t.light(), t.hacker(), nameStyle)).append("</style>\n")
          .append("</head>\n<body>\n");

        // ── HEADER ────────────────────────────────────────────────────────────
        sb.append("<div class=\"resume-header\">\n");
        sb.append("<div class=\"header-left\">\n");
        String avatarUrl = b.user() != null ? b.user().getAvatarUrl() : null;
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            sb.append("<img class=\"avatar\" src=\"").append(esc(avatarUrl))
              .append("\" alt=\"").append(esc(initials)).append("\"/>\n");
        } else {
            sb.append("<div class=\"avatar\">").append(esc(initials)).append("</div>\n");
        }
        sb.append("<div class=\"header-info\">\n");
        sb.append("<h1 class=\"r-name\">").append(esc(displayName)).append("</h1>\n");
        if (b.portfolio().getTagline() != null && !b.portfolio().getTagline().isBlank())
            sb.append("<div class=\"r-title\">").append(esc(b.portfolio().getTagline())).append("</div>\n");
        sb.append("<div class=\"r-contacts\">\n");
        if (email != null && !email.isBlank())
            sb.append("<span class=\"contact-item\">").append(esc(email)).append("</span>\n");
        if (ghUser != null && !ghUser.isBlank())
            sb.append("<span class=\"contact-item\">github.com/").append(esc(ghUser)).append("</span>\n");
        sb.append("<span class=\"contact-item badge-pill\">")
          .append(b.projects().size()).append(" repos</span>\n");
        sb.append("</div>\n</div>\n</div>\n</div>\n");

        sb.append("<div class=\"container\">\n");

        // Professional Summary
        if (b.aboutContent() != null && !b.aboutContent().isBlank())
            sb.append(previewSection("Professional Summary",
                "<div class=\"summary-text\">" + nl2br(esc(b.aboutContent())) + "</div>",
                t.primary(), t.border(), t.hacker()));

        // Core Skills
        if (!b.skills().isEmpty())
            sb.append(previewSection("Core Skills",
                buildPreviewSkills(b.skills(), t.card(), t.border(), t.primary(),
                        t.pLight(), t.muted(), t.text(), t.hacker()),
                t.primary(), t.border(), t.hacker()));

        // Work Experience
        if (!b.experiences().isEmpty())
            sb.append(previewSection("Work Experience",
                buildPreviewExperience(b.experiences(), t.primary(), t.text(), t.sub(), t.muted(), t.border()),
                t.primary(), t.border(), t.hacker()));

        // Notable Projects
        if (!b.projects().isEmpty()) {
            List<Project> featured = b.projects().stream().filter(Project::isHighlighted).toList();
            List<Project> toShow = !featured.isEmpty() ? featured : b.projects().stream().limit(6).toList();
            sb.append(previewSection("Notable Projects",
                buildPreviewProjects(toShow, t.card(), t.border(), t.primary(),
                        t.pLight(), t.muted(), t.text(), t.sub(), t.hacker()),
                t.primary(), t.border(), t.hacker()));
        }

        // Education
        if (!b.educations().isEmpty())
            sb.append(previewSection("Education",
                buildPreviewEducation(b.educations(), t.card(), t.border(), t.primary(), t.text(), t.muted()),
                t.primary(), t.border(), t.hacker()));

        // Certifications
        if (!b.certifications().isEmpty())
            sb.append(previewSection("Licenses & Certifications",
                buildPreviewCertifications(b.certifications(), t.card(), t.border(), t.primary(), t.text(), t.muted()),
                t.primary(), t.border(), t.hacker()));

        sb.append("</div>\n");
        sb.append("<div class=\"footer\">Built with <span style=\"color:")
          .append(t.primary()).append("\">OpenFolio</span></div>\n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ─── Preview helpers ──────────────────────────────────────────────────────

    private String previewSection(String title, String content, String primary, String border, boolean hacker) {
        String dot = hacker ? "# " : "";
        return "<div class=\"r-section\">\n"
             + "<div class=\"sec-header\"><span class=\"sec-dot\">" + dot + "</span>"
             + "<span class=\"sec-title\">" + esc(title) + "</span></div>\n"
             + content + "\n</div>\n";
    }

    private String buildPreviewSkills(List<Skill> skills, String card, String border,
            String primary, String pLight, String muted, String text, boolean hacker) {
        Map<String, List<Skill>> byProf = new LinkedHashMap<>();
        for (Skill s : skills) {
            String prof = s.getProficiency() != null ? capitalize(s.getProficiency().name()) : "Other";
            byProf.computeIfAbsent(prof, k -> new ArrayList<>()).add(s);
        }
        StringBuilder sb = new StringBuilder("<div class=\"skills-container\">\n");
        for (String level : List.of("Expert", "Advanced", "Intermediate", "Beginner")) {
            List<Skill> group = byProf.get(level);
            if (group == null || group.isEmpty()) continue;
            sb.append("<div class=\"skill-row\">\n");
            sb.append("<span class=\"skill-level\">").append(esc(level)).append("</span>\n");
            sb.append("<div class=\"skill-chips\">");
            for (Skill s : group)
                sb.append("<span class=\"skill-chip chip-").append(level.toLowerCase()).append("\">")
                  .append(esc(s.getName())).append("</span>");
            sb.append("</div>\n</div>\n");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPreviewProjects(List<Project> projects, String card, String border,
            String primary, String pLight, String muted, String text, String sub, boolean hacker) {
        StringBuilder sb = new StringBuilder("<div class=\"proj-list\">\n");
        for (Project p : projects) {
            sb.append("<div class=\"proj-item\">\n");
            sb.append("<div class=\"proj-header\">\n");
            sb.append("<span class=\"proj-name\">").append(esc(p.getName())).append("</span>\n");
            if (p.getStars() > 0)
                sb.append("<span class=\"proj-stats\">").append(p.getStars()).append(" stars</span>\n");
            sb.append("</div>\n");
            if (p.getLanguages() != null && !p.getLanguages().isEmpty()) {
                sb.append("<div class=\"proj-langs\">");
                for (String lang : p.getLanguages())
                    sb.append("<span class=\"lang-pill\">").append(esc(lang)).append("</span>");
                sb.append("</div>\n");
            }
            if (p.getDescription() != null && !p.getDescription().isBlank())
                sb.append("<p class=\"proj-desc\">").append(esc(p.getDescription())).append("</p>\n");
            if (p.getUrl() != null)
                sb.append("<a href=\"").append(esc(p.getUrl()))
                  .append("\" class=\"proj-link\" target=\"_blank\">View on GitHub</a>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPreviewExperience(List<Experience> exps, String primary,
            String text, String sub, String muted, String border) {
        StringBuilder sb = new StringBuilder("<div class=\"exp-list\">\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (Experience e : exps) {
            sb.append("<div class=\"exp-item\">\n");
            sb.append("<div class=\"exp-header\">\n<div>\n");
            sb.append("<div class=\"exp-role\">").append(esc(e.getTitle())).append("</div>\n");
            sb.append("<div class=\"exp-company\">").append(esc(e.getCompany())).append("</div>\n");
            sb.append("</div>\n");
            String start = e.getStartDate() != null ? e.getStartDate().format(fmt) : "";
            String end = e.isCurrent() ? "Present" : (e.getEndDate() != null ? e.getEndDate().format(fmt) : "");
            if (!start.isBlank())
                sb.append("<div class=\"exp-dates\">").append(esc(start)).append(" - ").append(esc(end)).append("</div>\n");
            sb.append("</div>\n");
            if (e.getDescription() != null && !e.getDescription().isBlank())
                sb.append("<p class=\"exp-desc\">").append(esc(e.getDescription())).append("</p>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPreviewEducation(List<Education> edus, String card, String border,
            String primary, String text, String muted) {
        StringBuilder sb = new StringBuilder("<div class=\"edu-list\">\n");
        for (Education e : edus) {
            sb.append("<div class=\"edu-item\">\n");
            String deg = ((e.getDegree() != null ? e.getDegree() : "") +
                         (e.getField() != null ? " in " + e.getField() : "")).trim();
            if (!deg.isBlank()) sb.append("<div class=\"edu-degree\">").append(esc(deg)).append("</div>\n");
            sb.append("<div class=\"edu-inst\">").append(esc(e.getInstitution())).append("</div>\n");
            String yrs = (e.getStartYear() != null ? e.getStartYear() : "") +
                         (e.getEndYear() != null ? " - " + e.getEndYear() : "");
            if (!yrs.isBlank()) sb.append("<div class=\"edu-dates\">").append(esc(yrs)).append("</div>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPreviewCertifications(List<Certification> certs, String card, String border,
            String primary, String text, String muted) {
        StringBuilder sb = new StringBuilder("<div class=\"cert-list\">\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (Certification c : certs) {
            sb.append("<div class=\"cert-item\">\n");
            sb.append("<div class=\"cert-name\">").append(esc(c.getName())).append("</div>\n");
            if (c.getIssuingOrganization() != null && !c.getIssuingOrganization().isBlank())
                sb.append("<div class=\"cert-org\">").append(esc(c.getIssuingOrganization())).append("</div>\n");
            String issued = c.getIssueDate() != null ? "Issued " + c.getIssueDate().format(fmt) : "";
            String expires = c.getExpiryDate() != null ? " · Expires " + c.getExpiryDate().format(fmt) : "";
            if (!issued.isBlank())
                sb.append("<div class=\"cert-dates\">").append(esc(issued + expires)).append("</div>\n");
            if (c.getCredentialId() != null && !c.getCredentialId().isBlank())
                sb.append("<div class=\"cert-cred\">Credential ID: ").append(esc(c.getCredentialId())).append("</div>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String previewCss(String bg, String surface, String card, String text,
            String sub, String muted, String primary, String pLight, String border,
            String font, boolean light, boolean hacker, String nameStyle) {
        return "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}"
             + "body{background:" + bg + ";color:" + text + ";font-family:" + font + ";font-size:15px;line-height:1.65}"
             + "a{color:" + primary + ";text-decoration:none}"
             + ".resume-header{background:" + surface + ";padding:36px 28px 28px;border-bottom:1px solid " + border + "}"
             + ".header-left{display:flex;align-items:flex-start;gap:20px}"
             + "img.avatar{width:68px;height:68px;border-radius:50%;object-fit:cover;border:2px solid " + border + "}"
             + ".avatar{width:68px;height:68px;border-radius:50%;background:"
             + (hacker ? "transparent;border:2px solid " + primary
                       : "linear-gradient(135deg," + primary + " 0%,#06B6D4 100%)")
             + ";display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:800;color:"
             + (hacker ? primary : "#fff") + ";flex-shrink:0}"
             + ".header-info{flex:1}"
             + ".r-name{font-size:clamp(22px,5vw,34px);font-weight:800;letter-spacing:-0.5px;" + nameStyle + ";line-height:1.1;margin-bottom:6px}"
             + ".r-title{font-size:15px;color:" + pLight + ";font-weight:600;margin-bottom:10px}"
             + ".r-contacts{display:flex;flex-wrap:wrap;gap:12px;align-items:center}"
             + ".contact-item{font-size:12px;color:" + muted + ";display:flex;align-items:center;gap:4px}"
             + ".badge-pill{background:" + rgba(primary, 0.12f) + ";color:" + pLight + ";padding:3px 10px;border-radius:100px;font-weight:600;border:1px solid " + rgba(primary, 0.2f) + "}"
             + ".container{max-width:780px;margin:0 auto;padding:0 28px 40px}"
             + ".r-section{padding:28px 0;border-top:1px solid " + border + "}"
             + ".sec-header{display:flex;align-items:center;gap:6px;margin-bottom:16px}"
             + ".sec-dot{color:" + primary + ";font-size:9px}"
             + ".sec-title{font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:2.5px;color:" + primary + "}"
             + ".summary-text{font-size:15px;color:" + sub + ";line-height:1.8}"
             + ".skills-container{display:flex;flex-direction:column;gap:10px}"
             + ".skill-row{display:flex;align-items:flex-start;gap:14px}"
             + ".skill-level{width:90px;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.8px;color:" + muted + ";padding-top:6px;flex-shrink:0}"
             + ".skill-chips{display:flex;flex-wrap:wrap;gap:6px}"
             + ".skill-chip{font-size:12px;font-weight:600;padding:5px 12px;border-radius:100px;border:1px solid}"
             + ".chip-expert{background:rgba(245,158,11,0.1);color:#F59E0B;border-color:rgba(245,158,11,0.28)}"
             + ".chip-advanced{background:rgba(167,139,250,0.1);color:#A78BFA;border-color:rgba(167,139,250,0.28)}"
             + ".chip-intermediate{background:rgba(52,211,153,0.1);color:#34D399;border-color:rgba(52,211,153,0.28)}"
             + ".chip-beginner{background:rgba(96,165,250,0.1);color:#60A5FA;border-color:rgba(96,165,250,0.28)}"
             + (light ? ".chip-expert{background:#FFFBEB;color:#92400E;border-color:#FDE68A}"
                      + ".chip-advanced{background:#F5F3FF;color:#6D28D9;border-color:#DDD6FE}"
                      + ".chip-intermediate{background:#F0FDF4;color:#166534;border-color:#BBF7D0}"
                      + ".chip-beginner{background:#EFF6FF;color:#1D4ED8;border-color:#BFDBFE}" : "")
             + ".exp-list{display:flex;flex-direction:column;gap:20px}"
             + ".exp-item{padding-left:16px;border-left:2px solid " + rgba(primary, 0.3f) + ";position:relative}"
             + ".exp-item::before{content:'';position:absolute;left:-5px;top:6px;width:8px;height:8px;background:" + primary + ";border-radius:50%}"
             + ".exp-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:4px}"
             + ".exp-role{font-size:15px;font-weight:700;color:" + text + "}"
             + ".exp-company{font-size:13px;color:" + pLight + ";margin-top:2px}"
             + ".exp-dates{font-size:12px;color:" + muted + ";white-space:nowrap;margin-left:12px;padding-top:2px}"
             + ".exp-desc{font-size:13px;color:" + sub + ";margin-top:8px;line-height:1.7}"
             + ".proj-list{display:flex;flex-direction:column;gap:16px}"
             + ".proj-item{background:" + card + ";border:1px solid " + border + ";border-radius:12px;padding:16px 18px}"
             + ".proj-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:6px}"
             + ".proj-name{font-size:15px;font-weight:700;color:" + text + "}"
             + ".proj-stats{font-size:12px;color:" + muted + "}"
             + ".proj-langs{display:flex;flex-wrap:wrap;gap:5px;margin-bottom:8px}"
             + ".lang-pill{background:" + rgba(primary, 0.1f) + ";color:" + pLight + ";font-size:11px;font-weight:600;padding:2px 8px;border-radius:100px;border:1px solid " + rgba(primary, 0.2f) + "}"
             + ".proj-desc{font-size:13px;color:" + sub + ";line-height:1.65;margin-bottom:6px}"
             + ".proj-link{font-size:12px;color:" + primary + ";font-weight:600}"
             + ".edu-list{display:flex;flex-direction:column;gap:12px}"
             + ".edu-item{padding:14px 16px;background:" + card + ";border:1px solid " + border + ";border-radius:10px}"
             + ".edu-degree{font-size:15px;font-weight:700;color:" + text + "}"
             + ".edu-inst{font-size:13px;color:" + pLight + ";margin-top:3px}"
             + ".edu-dates{font-size:12px;color:" + muted + ";margin-top:3px}"
             + ".cert-list{display:flex;flex-direction:column;gap:12px}"
             + ".cert-item{padding:14px 16px;background:" + card + ";border:1px solid " + border + ";border-radius:10px}"
             + ".cert-name{font-size:15px;font-weight:700;color:" + text + "}"
             + ".cert-org{font-size:13px;color:" + pLight + ";margin-top:3px}"
             + ".cert-dates{font-size:12px;color:" + muted + ";margin-top:3px}"
             + ".cert-cred{font-size:11px;color:" + muted + ";margin-top:2px;font-style:italic}"
             + ".footer{text-align:center;padding:24px;font-size:12px;color:" + muted + ";border-top:1px solid " + border + "}";
    }

    // =========================================================================
    //  PDF  (openhtmltopdf — XHTML 1.0 Strict, CSS 2.1, table-based layout)
    //  Modeled 1:1 after Enhancv premium resume templates
    // =========================================================================

    /** Clean summary — strip extra whitespace but keep full content. */
    private String cleanSummary(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.strip().replaceAll("\\s+", " ");
    }

    /** Build contact line: phone | email | linkedin | github | website. */
    private String contactLine(String email, String ghUser, ExportOptions options) {
        List<String> parts = new ArrayList<>();
        if (options.includePhone() && options.phone() != null && !options.phone().isBlank())
            parts.add(esc(options.phone()));
        if (email != null && !email.isBlank()) parts.add(esc(email));
        if (options.includeLinkedIn() && options.linkedIn() != null && !options.linkedIn().isBlank()) {
            String li = options.linkedIn();
            if (!li.startsWith("http")) li = "linkedin.com/in/" + li;
            parts.add(esc(li));
        }
        if (ghUser != null && !ghUser.isBlank()) parts.add("github.com/" + esc(ghUser));
        if (options.includeWebsite() && options.website() != null && !options.website().isBlank())
            parts.add(esc(options.website()));
        return String.join("  |  ", parts);
    }

    private String buildPdf(PortfolioBundle b, String templateKey, ExportOptions options) {
        String displayName = resolveDisplayName(b);
        String tagline     = b.portfolio().getTagline() != null ? b.portfolio().getTagline() : "";
        String email       = b.user() != null ? b.user().getEmail() : "";
        String ghUser      = b.user() != null ? b.user().getGithubUsername() : null;
        String summary     = cleanSummary(b.aboutContent());

        String photoUrl = null;
        if (options.includePhoto()) {
            if (options.photoUrl() != null && !options.photoUrl().isBlank())
                photoUrl = options.photoUrl();
            else if (b.user() != null && b.user().getAvatarUrl() != null)
                photoUrl = b.user().getAvatarUrl();
        }

        return switch (templateKey) {
            case "dark"    -> pdfTwoColumn(b, displayName, tagline, email, ghUser, summary, photoUrl, options, darkPalette());
            case "hacker"  -> pdfSingleColumn(b, displayName, tagline, email, ghUser, summary, photoUrl, options, hackerPalette());
            case "minimal" -> pdfSingleColumn(b, displayName, tagline, email, ghUser, summary, photoUrl, options, minimalPalette());
            default        -> pdfTwoColumn(b, displayName, tagline, email, ghUser, summary, photoUrl, options, cleanPalette());
        };
    }

    // ─── Color palette records ────────────────────────────────────────────────

    record Palette(
        String name,
        // Header
        String headerBg, String headerText, String headerTagline, String headerContact,
        // Accents
        String accent, String accentLight,
        // Body
        String bodyBg, String bodyText, String bodyMuted, String bodyFaint,
        // Dividers
        String divider, String dividerLight,
        // Font
        String fontFamily,
        // Flags
        boolean isDark, boolean isMonospace
    ) {}

    private Palette cleanPalette() {
        return new Palette("Clean",
            "#1B2A4A", "#FFFFFF", "#B0C4DE", "#8BA4C7",
            "#2D5BFF", "#4A7AFF",
            "#FFFFFF", "#2D2D2D", "#666666", "#999999",
            "#E0E0E0", "#F0F0F0",
            "'Helvetica Neue',Helvetica,Arial,sans-serif",
            false, false);
    }

    private Palette darkPalette() {
        return new Palette("Dark",
            "#1A1A2E", "#FFFFFF", "#A78BFA", "#71717A",
            "#A78BFA", "#C4B5FD",
            "#111118", "#D4D4D8", "#A1A1AA", "#71717A",
            "#2D2D3F", "#1E1E2E",
            "'Helvetica Neue',Helvetica,Arial,sans-serif",
            true, false);
    }

    private Palette minimalPalette() {
        return new Palette("Minimal",
            "#FFFFFF", "#111111", "#6B21A8", "#999999",
            "#6B21A8", "#7C3AED",
            "#FFFFFF", "#222222", "#666666", "#999999",
            "#DDDDDD", "#EEEEEE",
            "Georgia,'Times New Roman',serif",
            false, false);
    }

    private Palette hackerPalette() {
        return new Palette("Hacker",
            "#000000", "#00FF41", "#008F2C", "#006B20",
            "#00FF41", "#00CC33",
            "#000000", "#00CC33", "#008F2C", "#006B20",
            "#004D15", "#003310",
            "'Courier New','Lucida Console',monospace",
            true, true);
    }

    // =========================================================================
    //  PDF: Two-column layout (Clean & Dark templates — Enhancv-style)
    // =========================================================================

    private String pdfTwoColumn(PortfolioBundle b, String name, String tagline,
                                 String email, String ghUser, String summary,
                                 String photoUrl, ExportOptions options, Palette p) {
        StringBuilder sb = pdfDocStart(name, twoColumnCss(p));
        List<Project> projects = topProjects(b);

        // ── HEADER BANNER ─────────────────────────────────────────────────────
        sb.append("<div class=\"header\">\n");
        sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
        if (photoUrl != null) {
            sb.append("<td style=\"width:62pt;vertical-align:middle;padding-right:14pt\">");
            sb.append("<img src=\"").append(esc(photoUrl)).append("\" width=\"56\" height=\"56\" ");
            sb.append("style=\"border-radius:50%;border:2pt solid ").append(p.accent()).append("\" />");
            sb.append("</td>\n");
        }
        sb.append("<td style=\"vertical-align:middle\">");
        sb.append("<div class=\"h-name\">").append(esc(name.toUpperCase())).append("</div>\n");
        if (!tagline.isBlank())
            sb.append("<div class=\"h-tagline\">").append(esc(tagline)).append("</div>\n");
        sb.append("</td>\n");
        // Contact stacked on the right
        String cl = contactLine(email, ghUser, options);
        if (!cl.isBlank()) {
            sb.append("<td style=\"vertical-align:middle;text-align:right\">");
            for (String part : cl.split("\\s+\\|\\s+"))
                sb.append("<div class=\"h-contact\">").append(part).append("</div>\n");
            sb.append("</td>\n");
        }
        sb.append("</tr></table>\n</div>\n");

        // ── TWO-COLUMN BODY ───────────────────────────────────────────────────
        sb.append("<table class=\"body-tbl\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n<tr>\n");

        // ── LEFT COLUMN: Summary, Experience, Projects ────────────────────────
        sb.append("<td class=\"col-main\">\n");

        if (!summary.isBlank()) {
            sb.append(secHead("SUMMARY", p.accent()));
            sb.append("<p class=\"sum\">").append(esc(summary)).append("</p>\n");
        }

        if (!b.experiences().isEmpty()) {
            sb.append(secHead("EXPERIENCE", p.accent()));
            sb.append(expBlocks(b.experiences(), p));
        }

        if (!projects.isEmpty()) {
            sb.append(secHead("MY PROJECTS", p.accent()));
            sb.append(projBlocks(projects, p));
        }

        sb.append("</td>\n");

        // ── RIGHT COLUMN: Skills, Education, GitHub Stats ─────────────────────
        sb.append("<td class=\"col-side\">\n");

        if (!b.skills().isEmpty()) {
            sb.append(secHead("SKILLS", p.accent()));
            sb.append(skillTags(b.skills(), p));
        }

        if (!b.educations().isEmpty()) {
            sb.append(secHead("EDUCATION", p.accent()));
            sb.append(eduBlocks(b.educations(), p));
        }

        if (!b.certifications().isEmpty()) {
            sb.append(secHead("CERTIFICATIONS", p.accent()));
            sb.append(certBlocks(b.certifications(), p));
        }

        // GitHub stats
        if (!projects.isEmpty()) {
            sb.append(secHead("GITHUB", p.accent()));
            int totalStars = b.projects().stream().mapToInt(Project::getStars).sum();
            int totalForks = b.projects().stream().mapToInt(Project::getForks).sum();
            sb.append("<div class=\"gh-stat\">");
            sb.append("<span class=\"gh-val\">").append(b.projects().size()).append("</span> ");
            sb.append("<span class=\"gh-lbl\">Repositories</span></div>\n");
            if (totalStars > 0) {
                sb.append("<div class=\"gh-stat\">");
                sb.append("<span class=\"gh-val\">").append(totalStars).append("</span> ");
                sb.append("<span class=\"gh-lbl\">Total Stars</span></div>\n");
            }
            if (totalForks > 0) {
                sb.append("<div class=\"gh-stat\">");
                sb.append("<span class=\"gh-val\">").append(totalForks).append("</span> ");
                sb.append("<span class=\"gh-lbl\">Total Forks</span></div>\n");
            }
            // Top languages
            List<String> allLangs = b.projects().stream()
                .filter(pr -> pr.getLanguages() != null)
                .flatMap(pr -> pr.getLanguages().stream())
                .distinct().limit(8).toList();
            if (!allLangs.isEmpty()) {
                sb.append("<div class=\"gh-langs\">");
                for (String lang : allLangs)
                    sb.append("<span class=\"lang-tag\">").append(esc(lang)).append("</span> ");
                sb.append("</div>\n");
            }
        }

        sb.append("</td>\n");
        sb.append("</tr>\n</table>\n</body>\n</html>");
        return sb.toString();
    }

    private String twoColumnCss(Palette p) {
        return "@page{size:A4;margin:0}"
            + "body{font-family:" + p.fontFamily() + ";font-size:9pt;color:" + p.bodyText()
            + ";line-height:1.42;margin:0;padding:0"
            + (p.isDark() ? ";background:" + p.bodyBg() : "") + "}"

            // Header
            + ".header{background:" + p.headerBg() + ";padding:18pt 28pt;color:" + p.headerText() + "}"
            + ".h-name{font-size:20pt;font-weight:bold;color:" + p.headerText()
            + ";letter-spacing:2pt;line-height:1.15}"
            + ".h-tagline{font-size:9pt;color:" + p.headerTagline() + ";margin-top:2pt;letter-spacing:0.3pt}"
            + ".h-contact{font-size:7pt;color:" + p.headerContact() + ";line-height:1.7}"

            // Body columns
            + ".body-tbl{margin:0;padding:0}"
            + ".col-main{width:64%;vertical-align:top;padding:12pt 14pt 18pt 28pt}"
            + ".col-side{width:36%;vertical-align:top;padding:12pt 28pt 18pt 12pt;"
            + "border-left:0.5pt solid " + p.divider() + "}"

            // Section header
            + ".sec-h{margin:10pt 0 5pt;padding-bottom:2pt;border-bottom:1.2pt solid " + p.accent() + "}"
            + ".sec-l{font-size:7pt;font-weight:bold;letter-spacing:2pt;color:" + p.accent()
            + ";text-transform:uppercase}"

            // Summary
            + ".sum{font-size:9pt;color:" + p.bodyMuted() + ";line-height:1.6;margin:0 0 3pt}"

            // Experience
            + ".exp{margin-bottom:9pt;page-break-inside:avoid}"
            + ".exp-title{font-size:10pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".exp-co{color:" + p.accent() + ";font-weight:normal;font-size:9.5pt}"
            + ".exp-dt{font-size:7pt;color:" + p.bodyFaint() + ";text-align:right;vertical-align:top;white-space:nowrap}"
            + ".exp-loc{font-size:7pt;color:" + p.bodyFaint() + ";text-align:right}"
            + ".bullet-list{margin:2pt 0 0 0;padding:0}"
            + ".bullet{font-size:8.5pt;color:" + p.bodyMuted()
            + ";line-height:1.5;margin:1.5pt 0 0 8pt;padding:0}"

            // Projects
            + ".prj{margin-bottom:7pt;page-break-inside:avoid}"
            + ".prj-name{font-size:9.5pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".prj-lang{font-size:7.5pt;color:" + p.bodyFaint() + ";font-weight:normal}"
            + ".prj-meta{font-size:7pt;color:" + p.bodyFaint() + ";text-align:right;vertical-align:top;white-space:nowrap}"
            + ".prj-desc{font-size:8.5pt;color:" + p.bodyMuted() + ";line-height:1.5;margin:1.5pt 0 0 8pt}"
            + ".prj-link{font-size:7pt;color:" + p.accent() + ";margin-left:8pt}"

            // Skills
            + ".sk-group{margin-bottom:5pt}"
            + ".sk-label{font-size:6.5pt;font-weight:bold;color:" + p.accent()
            + ";text-transform:uppercase;letter-spacing:1pt;margin-bottom:2pt}"
            + ".sk-tag{display:inline;font-size:7.5pt;color:" + p.bodyText()
            + ";padding:1.5pt 5pt;margin:0 2pt 2pt 0"
            + ";background:" + (p.isDark() ? p.dividerLight() : "#F3F4F6")
            + ";border:0.3pt solid " + p.divider() + "}"

            // Education
            + ".edu{margin-bottom:7pt;page-break-inside:avoid}"
            + ".edu-deg{font-size:9pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".edu-inst{font-size:8pt;color:" + p.accent() + ";margin-top:1pt}"
            + ".edu-yr{font-size:7pt;color:" + p.bodyFaint() + ";margin-top:1pt}"

            // Certifications
            + ".cert{margin-bottom:6pt;page-break-inside:avoid}"
            + ".cert-name{font-size:8.5pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".cert-org{font-size:7.5pt;color:" + p.accent() + ";margin-top:1pt}"
            + ".cert-dt{font-size:7pt;color:" + p.bodyFaint() + ";margin-top:1pt}"

            // GitHub stats
            + ".gh-stat{margin-bottom:3pt}"
            + ".gh-val{font-size:11pt;font-weight:bold;color:" + p.accent() + "}"
            + ".gh-lbl{font-size:7.5pt;color:" + p.bodyFaint() + ";margin-left:2pt}"
            + ".gh-langs{margin-top:5pt}"
            + ".lang-tag{display:inline;font-size:7pt;color:" + p.bodyMuted()
            + ";padding:1pt 4pt;margin:0 2pt 2pt 0;background:" + (p.isDark() ? p.dividerLight() : "#F3F4F6")
            + ";border:0.3pt solid " + p.divider() + "}";
    }

    // =========================================================================
    //  PDF: Single-column layout (Minimal & Hacker templates)
    // =========================================================================

    private String pdfSingleColumn(PortfolioBundle b, String name, String tagline,
                                    String email, String ghUser, String summary,
                                    String photoUrl, ExportOptions options, Palette p) {
        boolean isHacker = p.isMonospace();
        StringBuilder sb = pdfDocStart(name, singleColumnCss(p));
        List<Project> projects = topProjects(b);

        // ── HEADER ────────────────────────────────────────────────────────────
        sb.append("<div class=\"header\">\n");
        sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>\n");
        if (photoUrl != null) {
            sb.append("<td style=\"width:58pt;vertical-align:top;padding-right:12pt\">");
            sb.append("<img src=\"").append(esc(photoUrl)).append("\" width=\"52\" height=\"52\" ");
            sb.append("style=\"border-radius:").append(isHacker ? "4pt" : "50%")
              .append(";border:").append(isHacker ? "1.5pt" : "0.5pt").append(" solid ")
              .append(isHacker ? p.accent() : "#ccc").append("\" />");
            sb.append("</td>\n");
        }
        sb.append("<td style=\"vertical-align:top\">");
        String nameText = isHacker ? "$ " + name : name;
        sb.append("<div class=\"h-name\">").append(esc(nameText)).append("</div>\n");
        if (!tagline.isBlank()) {
            String tagText = isHacker ? "// " + tagline : tagline;
            sb.append("<div class=\"h-tagline\">").append(esc(tagText)).append("</div>\n");
        }
        String cl = contactLine(email, ghUser, options);
        if (!cl.isBlank())
            sb.append("<div class=\"h-contact\">").append(cl).append("</div>\n");
        sb.append("</td>\n</tr></table>\n");
        sb.append("</div>\n");

        // ── BODY ──────────────────────────────────────────────────────────────

        // Summary
        if (!summary.isBlank()) {
            sb.append(secHead(isHacker ? "README.md" : "SUMMARY", p.accent()));
            sb.append("<p class=\"sum\">").append(esc(summary)).append("</p>\n");
        }

        // Skills — inline format: "Strong: Java, Python | Proficient: React, Go"
        if (!b.skills().isEmpty()) {
            sb.append(secHead(isHacker ? "tech_stack" : "SKILLS", p.accent()));
            sb.append(skillsInlinePdf(b.skills(), p));
        }

        // Experience
        if (!b.experiences().isEmpty()) {
            sb.append(secHead(isHacker ? "work_history" : "EXPERIENCE", p.accent()));
            sb.append(expBlocks(b.experiences(), p));
        }

        // Projects
        if (!projects.isEmpty()) {
            sb.append(secHead(isHacker ? "repositories" : "PROJECTS", p.accent()));
            sb.append(projBlocks(projects, p));
        }

        // Education
        if (!b.educations().isEmpty()) {
            sb.append(secHead(isHacker ? "education" : "EDUCATION", p.accent()));
            sb.append(eduBlocksSingle(b.educations(), p));
        }

        // Certifications
        if (!b.certifications().isEmpty()) {
            sb.append(secHead(isHacker ? "certifications" : "CERTIFICATIONS", p.accent()));
            sb.append(certBlocksSingle(b.certifications(), p));
        }

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private String singleColumnCss(Palette p) {
        boolean isHacker = p.isMonospace();
        return "@page{size:A4;margin:" + (isHacker ? "0" : "36pt 44pt") + "}"
            + "body{font-family:" + p.fontFamily() + ";font-size:" + (isHacker ? "9pt" : "10pt")
            + ";color:" + p.bodyText() + ";line-height:1.5;margin:0;padding:"
            + (isHacker ? "28pt 40pt" : "0")
            + (p.isDark() ? ";background:" + p.bodyBg() : "") + "}"

            // Header
            + ".header{padding-bottom:10pt;margin-bottom:6pt;border-bottom:"
            + (isHacker ? "1.5pt solid " + p.accent() : "0.5pt solid " + p.divider()) + "}"
            + ".h-name{font-size:" + (isHacker ? "18pt" : "24pt") + ";font-weight:"
            + (isHacker ? "bold" : "normal") + ";color:" + p.headerText()
            + (isHacker ? ";letter-spacing:2pt" : ";letter-spacing:0.5pt") + "}"
            + ".h-tagline{font-size:9.5pt;color:" + p.headerTagline() + ";margin-top:3pt"
            + (isHacker ? ";letter-spacing:0.5pt" : ";font-style:italic") + "}"
            + ".h-contact{font-size:8pt;color:" + p.headerContact() + ";margin-top:5pt}"

            // Section header
            + ".sec-h{margin:12pt 0 5pt;padding-bottom:2pt;border-bottom:"
            + (isHacker ? "1pt dashed " + p.divider() : "0.5pt solid " + p.divider()) + "}"
            + ".sec-l{font-size:7.5pt;font-weight:" + (isHacker ? "bold" : "normal")
            + (isHacker ? "" : ";font-style:italic")
            + ";letter-spacing:2pt;color:" + p.accent() + ";text-transform:uppercase}"

            // Summary
            + ".sum{font-size:9.5pt;color:" + p.bodyMuted() + ";line-height:1.65;margin:0 0 3pt}"

            // Skills inline
            + ".sk-line{font-size:9.5pt;color:" + p.bodyMuted() + ";line-height:1.65;margin:0 0 2pt}"

            // Experience
            + ".exp{margin-bottom:10pt;page-break-inside:avoid}"
            + ".exp-title{font-size:10.5pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".exp-co{color:" + p.accent() + ";font-weight:normal}"
            + ".exp-dt{font-size:7.5pt;color:" + p.bodyFaint()
            + ";text-align:right;vertical-align:top;white-space:nowrap}"
            + ".bullet-list{margin:2pt 0 0 0;padding:0}"
            + ".bullet{font-size:9pt;color:" + p.bodyMuted()
            + ";line-height:1.55;margin:1.5pt 0 0 10pt;padding:0}"

            // Projects
            + ".prj{margin-bottom:8pt;page-break-inside:avoid}"
            + ".prj-name{font-size:10pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".prj-lang{font-size:8pt;color:" + p.bodyFaint() + ";font-weight:normal}"
            + ".prj-meta{font-size:7.5pt;color:" + p.bodyFaint()
            + ";text-align:right;vertical-align:top;white-space:nowrap}"
            + ".prj-desc{font-size:9pt;color:" + p.bodyMuted() + ";line-height:1.55;margin:1.5pt 0 0 10pt}"
            + ".prj-link{font-size:7.5pt;color:" + p.accent() + ";margin-left:10pt}"

            // Education
            + ".edu{margin-bottom:7pt;page-break-inside:avoid}"
            + ".edu-deg{font-size:10pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".edu-inst{font-size:9pt;color:" + p.accent() + ";margin-top:1pt}"
            + ".edu-yr{font-size:8pt;color:" + p.bodyFaint() + "}"

            // Certifications
            + ".cert{margin-bottom:7pt;page-break-inside:avoid}"
            + ".cert-name{font-size:10pt;font-weight:bold;color:" + p.bodyText() + "}"
            + ".cert-org{font-size:9pt;color:" + p.accent() + ";margin-top:1pt}"
            + ".cert-dt{font-size:8pt;color:" + p.bodyFaint() + "}"
            + ".cert-cred{font-size:7pt;color:" + p.bodyFaint() + ";font-style:italic;margin-top:1pt}";
    }

    // =========================================================================
    //  Shared PDF content blocks (used by both layouts)
    // =========================================================================

    private StringBuilder pdfDocStart(String title, String css) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ")
          .append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
          .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n<head>\n")
          .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
          .append("<title>").append(esc(title)).append(" - Resume</title>\n")
          .append("<style>\n").append(css).append("\n</style>\n")
          .append("</head>\n<body>\n");
        return sb;
    }

    private String secHead(String title, String accent) {
        return "<div class=\"sec-h\"><span class=\"sec-l\">"
             + esc(title) + "</span></div>\n";
    }

    // ── Experience ────────────────────────────────────────────────────────────

    private String expBlocks(List<Experience> exps, Palette p) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        StringBuilder sb = new StringBuilder();
        for (Experience e : exps) {
            sb.append("<div class=\"exp\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>");
            sb.append("<td class=\"exp-title\">").append(esc(e.getTitle()));
            sb.append("  <span class=\"exp-co\">").append(esc(e.getCompany())).append("</span></td>");
            String start = e.getStartDate() != null ? e.getStartDate().format(fmt) : "";
            String end = e.isCurrent() ? "Present" : (e.getEndDate() != null ? e.getEndDate().format(fmt) : "");
            String dates = start + (!end.isBlank() ? " - " + end : "");
            sb.append("<td class=\"exp-dt\">").append(esc(dates)).append("</td>");
            sb.append("</tr></table>\n");
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("<div class=\"bullet-list\">");
                for (String bullet : splitBullets(e.getDescription())) {
                    if (!bullet.isBlank())
                        sb.append("<div class=\"bullet\">&#8226; ").append(esc(bullet.trim())).append("</div>\n");
                }
                sb.append("</div>\n");
            }
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    // ── Projects ──────────────────────────────────────────────────────────────

    private String projBlocks(List<Project> projects, Palette p) {
        StringBuilder sb = new StringBuilder();
        for (Project pr : projects) {
            sb.append("<div class=\"prj\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>");
            sb.append("<td class=\"prj-name\">").append(esc(pr.getName()));
            if (pr.getLanguages() != null && !pr.getLanguages().isEmpty())
                sb.append("  <span class=\"prj-lang\">")
                  .append(esc(String.join(", ", pr.getLanguages()))).append("</span>");
            sb.append("</td>");
            sb.append("<td class=\"prj-meta\">");
            List<String> meta = new ArrayList<>();
            if (pr.getStars() > 0) meta.add(pr.getStars() + " stars");
            if (pr.getForks() > 0) meta.add(pr.getForks() + " forks");
            sb.append(esc(String.join(" / ", meta)));
            sb.append("</td></tr></table>\n");

            // Description as bullet points
            if (pr.getDescription() != null && !pr.getDescription().isBlank()) {
                String[] bullets = splitBullets(pr.getDescription());
                if (bullets.length > 1) {
                    sb.append("<div class=\"bullet-list\">");
                    for (String bullet : bullets) {
                        if (!bullet.isBlank())
                            sb.append("<div class=\"prj-desc\">&#8226; ").append(esc(bullet.trim())).append("</div>\n");
                    }
                    sb.append("</div>\n");
                } else {
                    sb.append("<div class=\"prj-desc\">").append(esc(pr.getDescription())).append("</div>\n");
                }
            }

            // GitHub link
            if (pr.getUrl() != null && !pr.getUrl().isBlank())
                sb.append("<div class=\"prj-link\">GitHub: ").append(esc(pr.getUrl())).append("</div>\n");
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    // ── Skills (tag-based, for sidebar) ───────────────────────────────────────

    private String skillTags(List<Skill> skills, Palette p) {
        Map<String, List<Skill>> byProf = groupSkills(skills);
        StringBuilder sb = new StringBuilder();
        for (String level : List.of("Expert", "Advanced", "Intermediate", "Beginner")) {
            List<Skill> group = byProf.get(level);
            if (group == null || group.isEmpty()) continue;
            sb.append("<div class=\"sk-group\">\n");
            sb.append("<div class=\"sk-label\">").append(esc(level)).append("</div>\n");
            for (Skill s : group)
                sb.append("<span class=\"sk-tag\">").append(esc(s.getName())).append("</span> ");
            sb.append("\n</div>\n");
        }
        return sb.toString();
    }

    /** Skills as inline text (for single-column templates). */
    private String skillsInlinePdf(List<Skill> skills, Palette p) {
        Map<String, List<Skill>> byProf = groupSkills(skills);
        StringBuilder sb = new StringBuilder("<p class=\"sk-line\">");
        List<String> groups = new ArrayList<>();
        for (String level : List.of("Expert", "Advanced", "Intermediate", "Beginner")) {
            List<Skill> group = byProf.get(level);
            if (group == null || group.isEmpty()) continue;
            String names = group.stream().map(s -> esc(s.getName())).collect(Collectors.joining(", "));
            String label = switch (level) {
                case "Expert" -> "Strong";
                case "Advanced" -> "Proficient";
                case "Intermediate" -> "Familiar";
                default -> "Exposure";
            };
            groups.add("<b>" + label + ":</b> " + names);
        }
        sb.append(String.join("  |  ", groups));
        sb.append("</p>\n");
        return sb.toString();
    }

    // ── Education ─────────────────────────────────────────────────────────────

    /** Education for sidebar (compact). */
    private String eduBlocks(List<Education> edus, Palette p) {
        StringBuilder sb = new StringBuilder();
        for (Education e : edus) {
            sb.append("<div class=\"edu\">\n");
            String deg = degreeLine(e);
            sb.append("<div class=\"edu-deg\">").append(esc(deg.isBlank() ? e.getInstitution() : deg)).append("</div>\n");
            if (!deg.isBlank())
                sb.append("<div class=\"edu-inst\">").append(esc(e.getInstitution())).append("</div>\n");
            String yrs = yearRange(e);
            if (!yrs.isBlank())
                sb.append("<div class=\"edu-yr\">").append(esc(yrs)).append("</div>\n");
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    /** Education for single-column (with dates right-aligned). */
    private String eduBlocksSingle(List<Education> edus, Palette p) {
        StringBuilder sb = new StringBuilder();
        for (Education e : edus) {
            sb.append("<div class=\"edu\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>");
            String deg = degreeLine(e);
            sb.append("<td class=\"edu-deg\">").append(esc(deg.isBlank() ? e.getInstitution() : deg)).append("</td>");
            sb.append("<td class=\"edu-yr\" style=\"text-align:right;white-space:nowrap\">")
              .append(esc(yearRange(e))).append("</td>");
            sb.append("</tr></table>\n");
            if (!deg.isBlank())
                sb.append("<div class=\"edu-inst\">").append(esc(e.getInstitution())).append("</div>\n");
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    // ── Certifications ────────────────────────────────────────────────────

    /** Certifications for sidebar (compact). */
    private String certBlocks(List<Certification> certs, Palette p) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        StringBuilder sb = new StringBuilder();
        for (Certification c : certs) {
            sb.append("<div class=\"cert\">\n");
            sb.append("<div class=\"cert-name\">").append(esc(c.getName())).append("</div>\n");
            if (c.getIssuingOrganization() != null && !c.getIssuingOrganization().isBlank())
                sb.append("<div class=\"cert-org\">").append(esc(c.getIssuingOrganization())).append("</div>\n");
            String dt = c.getIssueDate() != null ? c.getIssueDate().format(fmt) : "";
            if (!dt.isBlank())
                sb.append("<div class=\"cert-dt\">").append(esc(dt)).append("</div>\n");
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    /** Certifications for single-column (with dates right-aligned). */
    private String certBlocksSingle(List<Certification> certs, Palette p) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        StringBuilder sb = new StringBuilder();
        for (Certification c : certs) {
            sb.append("<div class=\"cert\">\n");
            sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>");
            sb.append("<td class=\"cert-name\">").append(esc(c.getName()));
            if (c.getIssuingOrganization() != null && !c.getIssuingOrganization().isBlank())
                sb.append("  <span class=\"cert-org\">").append(esc(c.getIssuingOrganization())).append("</span>");
            sb.append("</td>");
            String dt = c.getIssueDate() != null ? c.getIssueDate().format(fmt) : "";
            sb.append("<td class=\"cert-dt\" style=\"text-align:right;white-space:nowrap\">").append(esc(dt)).append("</td>");
            sb.append("</tr></table>\n");
            if (c.getCredentialId() != null && !c.getCredentialId().isBlank())
                sb.append("<div class=\"cert-cred\">Credential ID: ").append(esc(c.getCredentialId())).append("</div>\n");
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    // =========================================================================
    //  Text processing helpers
    // =========================================================================

    /**
     * Split text into bullet points for resume formatting.
     * Splits on newlines, sentence boundaries, or returns as single item.
     */
    private String[] splitBullets(String text) {
        if (text == null || text.isBlank()) return new String[0];
        text = text.strip();

        // If contains newlines, split on those
        if (text.contains("\n")) {
            return Arrays.stream(text.split("\\n"))
                    .map(s -> s.replaceAll("^[-•*>]+\\s*", "").trim())
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
        }

        // If 2+ sentences, split at sentence boundaries
        String[] sentences = text.split("(?<=[.!?])\\s+(?=[A-Z])");
        if (sentences.length >= 2) {
            return Arrays.stream(sentences)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
        }

        return new String[]{ text };
    }

    /** Pick top projects — highlighted first, then by stars, max 8. */
    private List<Project> topProjects(PortfolioBundle b) {
        List<Project> highlighted = b.projects().stream().filter(Project::isHighlighted).toList();
        if (!highlighted.isEmpty()) return highlighted.stream().limit(8).toList();
        return b.projects().stream()
                .sorted((a, c) -> Integer.compare(c.getStars(), a.getStars()))
                .limit(8).toList();
    }

    private Map<String, List<Skill>> groupSkills(List<Skill> skills) {
        Map<String, List<Skill>> byProf = new LinkedHashMap<>();
        for (Skill s : skills) {
            String prof = s.getProficiency() != null ? capitalize(s.getProficiency().name()) : "Other";
            byProf.computeIfAbsent(prof, k -> new ArrayList<>()).add(s);
        }
        return byProf;
    }

    private String degreeLine(Education e) {
        return ((e.getDegree() != null ? e.getDegree() : "") +
                (e.getField() != null ? " in " + e.getField() : "")).trim();
    }

    private String yearRange(Education e) {
        return (e.getStartYear() != null ? String.valueOf(e.getStartYear()) : "") +
               (e.getEndYear() != null ? " - " + e.getEndYear() : "");
    }

    // =========================================================================
    //  Utilities
    // =========================================================================

    private String resolveDisplayName(PortfolioBundle b) {
        if (b.user() != null && b.user().getDisplayName() != null && !b.user().getDisplayName().isBlank())
            return b.user().getDisplayName();
        if (b.portfolio().getTitle() != null) return b.portfolio().getTitle();
        return "Developer";
    }

    private String initials(String name) {
        return Arrays.stream(name.split("\\s+"))
                .limit(2).filter(w -> !w.isBlank())
                .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
                .collect(Collectors.joining());
    }

    private String rgba(String hex, float alpha) {
        try {
            hex = hex.replace("#", "");
            if (hex.length() == 3)
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1)
                        + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int bv = Integer.parseInt(hex.substring(4, 6), 16);
            return String.format("rgba(%d,%d,%d,%.2f)", r, g, bv, alpha);
        } catch (Exception e) {
            return "#" + hex;
        }
    }

    private String esc(Object o) {
        if (o == null) return "";
        return o.toString()
                .replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String nl2br(String s) { return s == null ? "" : s.replace("\n", "<br/>"); }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}

