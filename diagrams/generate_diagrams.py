import os
import math
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
import numpy as np

OUTPUT = os.path.dirname(os.path.abspath(__file__))

# ── Color palette ─────────────────────────────────────────────────────────────
BG      = '#0D1117'
SURF    = '#161B22'
SURF2   = '#21262D'
BORDER  = '#30363D'
PURPLE  = '#8B5CF6'
BLUE    = '#3B82F6'
CYAN    = '#22D3EE'
GREEN   = '#22C55E'
AMBER   = '#F59E0B'
RED     = '#EF4444'
ORANGE  = '#F97316'
PINK    = '#EC4899'
LIME    = '#84CC16'
TEXT    = '#F0F6FC'
TEXTSUB = '#8B949E'
TEXTMUT = '#3D444D'

# ── Helpers ───────────────────────────────────────────────────────────────────

def save(fig, name):
    path = os.path.join(OUTPUT, name)
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=BG, edgecolor='none')
    plt.close(fig)
    print(f'  checkmark  {name}')


def make_fig(w=18, h=11, title='', subtitle=''):
    fig, ax = plt.subplots(figsize=(w, h), facecolor=BG)
    ax.set_facecolor(BG)
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')
    if title:
        ax.text(0.5, 0.978, title, ha='center', va='top',
                fontsize=16, fontweight='bold', color=TEXT, zorder=10)
    if subtitle:
        ax.text(0.5, 0.956, subtitle, ha='center', va='top',
                fontsize=9, color=TEXTSUB, zorder=10)
    return fig, ax


def rbox(ax, cx, cy, w, h, fill, label='', sub='', tc=TEXT,
         fs=9, sfs=7, border=None, bold=True, zorder=4):
    """Rounded rectangle centered at (cx, cy)."""
    p = FancyBboxPatch(
        (cx - w / 2, cy - h / 2), w, h,
        boxstyle='round,pad=0,rounding_size=0.010',
        linewidth=1.2, edgecolor=border or fill,
        facecolor=fill, zorder=zorder)
    ax.add_patch(p)
    if label:
        dy = 0.011 if sub else 0
        ax.text(cx, cy + dy, label, ha='center', va='center',
                fontsize=fs, fontweight='bold' if bold else 'normal',
                color=tc, zorder=zorder + 1, clip_on=False)
    if sub:
        ax.text(cx, cy - 0.014, sub, ha='center', va='center',
                fontsize=sfs, color=tc, alpha=0.75, zorder=zorder + 1, clip_on=False)


def group_box(ax, x, y, w, h, color, title='', fs=9):
    p = FancyBboxPatch((x, y), w, h,
        boxstyle='round,pad=0,rounding_size=0.012',
        linewidth=1.5, edgecolor=color + '55', facecolor=color + '0C', zorder=2)
    ax.add_patch(p)
    if title:
        ax.text(x + w / 2, y + h - 0.014, title,
                ha='center', va='top', fontsize=fs,
                fontweight='bold', color=color, zorder=5)


def arr(ax, x1, y1, x2, y2, c=TEXTSUB, lw=1.5, lbl='',
        lfs=7.5, rad=0.0, bi=False, head=10):
    style = '<->' if bi else '->'
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=c, lw=lw,
                                connectionstyle=f'arc3,rad={rad}',
                                mutation_scale=head), zorder=3)
    if lbl:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2 + 0.010
        ax.text(mx, my, lbl, ha='center', va='bottom', fontsize=lfs, color=c,
                zorder=6, bbox=dict(boxstyle='round,pad=0.15', fc=BG, ec='none', alpha=0.95))


def seq_bar(ax, x, y_top, y_bot, color, label, lfs=8.5):
    ax.plot([x, x], [y_bot, y_top], color=color, lw=1.5, linestyle='--',
            alpha=0.5, zorder=2)
    rbox(ax, x, y_top + 0.028, 0.13, 0.044, color, label, fs=lfs, bold=True)


def seq_msg(ax, x1, x2, y, color, label, lfs=7.5, dashed=False):
    ls = '--' if dashed else '-'
    ax.annotate('', xy=(x2, y), xytext=(x1, y),
                arrowprops=dict(arrowstyle='->', color=color, lw=1.5,
                                linestyle=ls, mutation_scale=10), zorder=3)
    mx = (x1 + x2) / 2
    ax.text(mx, y + 0.009, label, ha='center', va='bottom', fontsize=lfs,
            color=color, zorder=6,
            bbox=dict(boxstyle='round,pad=0.12', fc=BG, ec='none', alpha=0.92))


def diamond(ax, cx, cy, w, h, color, label, fs=9):
    pts = np.array([[cx, cy + h / 2], [cx + w / 2, cy],
                    [cx, cy - h / 2], [cx - w / 2, cy]])
    ax.add_patch(plt.Polygon(pts, closed=True,
                              facecolor=color + '28',
                              edgecolor=color + '88', linewidth=1.3, zorder=4))
    ax.text(cx, cy, label, ha='center', va='center',
            fontsize=fs, fontweight='bold', color=color, zorder=5)


# =============================================================================
# 1. System Architecture
# =============================================================================

def d1_system_architecture():
    fig, ax = make_fig(20, 12,
        title='OpenFolio  —  System Architecture',
        subtitle='React Native Mobile  |  Spring Boot REST API (Java 21)  |  MySQL  |  Ollama AI')

    # ── Mobile column ─────────────────────────────────────────────────────────
    group_box(ax, 0.02, 0.06, 0.21, 0.86, PURPLE, 'MOBILE APP', fs=10)
    ax.text(0.125, 0.880, 'React Native 0.84  iOS + Android', ha='center',
            fontsize=7.5, color=TEXTSUB, zorder=5)

    screens = [
        ('WelcomeScreen',        'GitHub OAuth login'),
        ('DashboardScreen',      'Portfolio list'),
        ('EditorScreen',         'Projects & Skills'),
        ('PreviewScreen',        'WebView preview'),
        ('PublishScreen',        'QR code & share'),
        ('ExportScreen',         'PDF templates'),
        ('ResumeBuilderScreen',  'Resume form'),
        ('ResumeEditorScreen',   'Exp / Edu / Cert'),
    ]
    for i, (name, sub) in enumerate(screens):
        cy = 0.820 - i * 0.088
        rbox(ax, 0.125, cy, 0.175, 0.070, SURF2, name, sub,
             fs=8, sfs=6.5, border=PURPLE + '44')

    rbox(ax, 0.125, 0.090, 0.185, 0.048, PURPLE + '18',
         'Zustand  |  Axios + JWT', 'Keychain token storage',
         fs=7.5, sfs=6.5, border=PURPLE + '55')

    # ── Backend column ────────────────────────────────────────────────────────
    group_box(ax, 0.26, 0.06, 0.46, 0.86, BLUE, 'SPRING BOOT API', fs=10)
    ax.text(0.49, 0.880, 'Java 21  |  Spring Boot 3.4.2  |  Flyway V001-V017',
            ha='center', fontsize=7.5, color=TEXTSUB, zorder=5)

    # Security bar
    rbox(ax, 0.49, 0.855, 0.43, 0.034, ORANGE + '22',
         'Spring Security  |  JwtAuthFilter  |  BCrypt  |  Stateless',
         fs=7.5, border=ORANGE + '55', bold=False)

    # 6 domain boxes — 2 rows of 3
    domains = [
        ('Auth', 'Register / OAuth / JWT', ORANGE),
        ('Portfolio', 'CRUD / HTML / Publish', BLUE),
        ('Resume', 'Builder / Templates', PURPLE),
        ('Export', 'PDF / Saved / Share', RED),
        ('Ingestion', 'GitHub import / AI', GREEN),
        ('Public API', 'Unauthenticated pages', PINK),
    ]
    dw, dh = 0.128, 0.095
    for i, (name, sub, color) in enumerate(domains):
        row, col = i // 3, i % 3
        cx = 0.316 + col * 0.148
        cy = 0.740 - row * 0.122
        rbox(ax, cx, cy, dw, dh, color + '25', name, sub,
             fs=8.5, sfs=7, border=color + '66')

    # Repository strip
    rbox(ax, 0.49, 0.497, 0.43, 0.042, SURF2,
         'Spring Data JPA Repositories  (12 repos, 12 entities)',
         fs=7.5, border=CYAN + '44', bold=False)

    # Shared strip
    rbox(ax, 0.49, 0.434, 0.43, 0.042, SURF2,
         'Shared:  ApiResponse<T>  |  JwtTokenProvider  |  GlobalExceptionHandler',
         fs=7.5, border=BORDER, bold=False)

    # HTML generators
    rbox(ax, 0.390, 0.370, 0.200, 0.042, SURF2,
         'PortfolioHtmlGenerator', 'dark / minimal / hacker',
         fs=8, sfs=6.5, border=BLUE + '44')
    rbox(ax, 0.606, 0.370, 0.200, 0.042, SURF2,
         'ResumeHtmlGenerator', 'classic / modern / minimal / bold',
         fs=8, sfs=6.5, border=PURPLE + '44')

    # PDF engine
    rbox(ax, 0.49, 0.305, 0.260, 0.042, SURF2,
         'openhtmltopdf  (HTML -> PDF)',
         fs=7.5, border=RED + '44', bold=False)

    # Temp store
    rbox(ax, 0.390, 0.240, 0.195, 0.042, SURF2,
         'ExportTempStore', 'UUID token, 10-min TTL',
         fs=8, sfs=6.5, border=AMBER + '44')

    # AI enhancer
    rbox(ax, 0.606, 0.240, 0.195, 0.042, GREEN + '18',
         'AiResumeEnhancer', 'OllamaClient (qwen2.5:14b)',
         fs=8, sfs=6.5, border=GREEN + '55')

    # GitHub client
    rbox(ax, 0.49, 0.175, 0.260, 0.042, AMBER + '18',
         'GitHubClient  (profile, repos, languages, README)',
         fs=7.5, border=AMBER + '55', bold=False)

    # ── DB + External column ──────────────────────────────────────────────────
    group_box(ax, 0.75, 0.45, 0.23, 0.47, CYAN, 'MYSQL  DATABASE', fs=10)
    ax.text(0.865, 0.880, 'Flyway V001-V017  |  12 tables', ha='center',
            fontsize=7.5, color=TEXTSUB, zorder=5)

    tables = ['users', 'auth_identities', 'portfolios', 'sections',
              'projects', 'skills', 'experiences', 'education',
              'certifications', 'resumes', 'saved_resumes', 'publish_records']
    for i, t in enumerate(tables):
        rbox(ax, 0.865, 0.865 - i * 0.038, 0.205, 0.030,
             SURF2, t, fs=7.5, border=CYAN + '33', bold=False)

    # External services
    rbox(ax, 0.865, 0.340, 0.210, 0.080, AMBER + '18',
         'GITHUB API', 'profile / repos / languages / readme',
         fs=9.5, sfs=7.5, border=AMBER + '66')

    rbox(ax, 0.865, 0.230, 0.210, 0.080, GREEN + '18',
         'OLLAMA AI', 'localhost:11434  |  qwen2.5:14b',
         fs=9.5, sfs=7.5, border=GREEN + '66')

    rbox(ax, 0.865, 0.120, 0.210, 0.070, PINK + '18',
         'PUBLIC WEB', '/api/v1/public/{slug}',
         fs=9.5, sfs=7.5, border=PINK + '66')

    # ── Arrows ────────────────────────────────────────────────────────────────
    arr(ax, 0.234, 0.50, 0.258, 0.50, c=PURPLE, lw=2.2, lbl='HTTPS / JWT', lfs=7.5, bi=True)
    arr(ax, 0.723, 0.64, 0.748, 0.64, c=CYAN, lw=1.8, lbl='JPA')
    arr(ax, 0.723, 0.340, 0.753, 0.340, c=AMBER, lw=1.8, lbl='REST')
    arr(ax, 0.723, 0.230, 0.753, 0.230, c=GREEN, lw=1.8, lbl='HTTP')
    # Public dashed
    ax.plot([0.723, 0.745, 0.745, 0.753],
            [0.50, 0.50, 0.120, 0.120],
            color=PINK, lw=1.4, linestyle='--', zorder=3)
    ax.annotate('', xy=(0.753, 0.120), xytext=(0.745, 0.120),
                arrowprops=dict(arrowstyle='->', color=PINK, lw=1.4), zorder=3)

    save(fig, '01_system_architecture.png')


# =============================================================================
# 2. Entity Relationship Diagram
# =============================================================================

def d2_erd():
    fig, ax = make_fig(24, 16,
        title='OpenFolio  —  Entity Relationship Diagram',
        subtitle='MySQL database schema  |  12 tables  |  Flyway migrations V001-V017')

    ROW_H = 0.026
    HDR_H = 0.038
    W     = 0.200

    def tbl_height(n_cols):
        return HDR_H + n_cols * ROW_H + 0.008

    def draw_table(cx, cy, name, columns, color):
        """Draw ERD table. cy is the TOP of the table (header top edge)."""
        n = len(columns)
        total_h = tbl_height(n)
        # Header
        ax.add_patch(FancyBboxPatch(
            (cx - W / 2, cy - HDR_H), W, HDR_H,
            boxstyle='round,pad=0,rounding_size=0.006',
            lw=0, fc=color, zorder=4))
        ax.text(cx, cy - HDR_H / 2, name,
                ha='center', va='center', fontsize=8.5,
                fontweight='bold', color='#fff', zorder=5)
        # Body
        body_h = n * ROW_H + 0.008
        body_y = cy - HDR_H - body_h
        ax.add_patch(FancyBboxPatch(
            (cx - W / 2, body_y), W, body_h,
            boxstyle='round,pad=0,rounding_size=0.006',
            lw=1.0, ec=color + '66', fc=SURF, zorder=4))
        for j, col in enumerate(columns):
            ry = cy - HDR_H - ROW_H * (j + 0.5) - 0.004
            # icon
            if col.startswith('PK'):
                icon, txt, ic = '*', col[2:].strip(), AMBER
            elif col.startswith('FK'):
                icon, txt, ic = '>', col[2:].strip(), CYAN
            else:
                icon, txt, ic = ' ', col.strip(), TEXTSUB
            ax.text(cx - W / 2 + 0.007, ry, icon,
                    ha='left', va='center', fontsize=7, color=ic, zorder=5)
            ax.text(cx - W / 2 + 0.022, ry, txt,
                    ha='left', va='center', fontsize=6.8, color=TEXT, zorder=5)
            if j < n - 1:
                sep_y = cy - HDR_H - ROW_H * (j + 1) - 0.002
                ax.plot([cx - W / 2 + 0.005, cx + W / 2 - 0.005],
                        [sep_y, sep_y], color=BORDER, lw=0.4, zorder=3)
        return body_y  # bottom y of the table

    def fk_arr(x1, y1, x2, y2, c):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=c + 'AA', lw=1.2,
                                    mutation_scale=9), zorder=3)

    # ── Layout: 4 columns × 3 rows ────────────────────────────────────────────
    # Column centers
    C1, C2, C3, C4 = 0.13, 0.37, 0.63, 0.87

    # We draw tables from their TOP edge. Reserve y=0.93 for first row top.
    # Row 1 tops at y=0.930
    # Row 2 tops calculated from heights
    # Row 3 similarly

    # ── ROW 1 ─────────────────────────────────────────────────────────────────
    # users (C1)
    draw_table(C1, 0.930, 'users', [
        'PK id BIGINT AUTO_INCREMENT',
        '   email VARCHAR(255) UNIQUE',
        '   display_name VARCHAR(255)',
        '   avatar_url TEXT',
        '   github_username VARCHAR',
        '   created_at DATETIME',
        '   updated_at DATETIME',
    ], BLUE)

    # auth_identities (C2)
    draw_table(C2, 0.930, 'auth_identities', [
        'PK id BIGINT',
        'FK user_id BIGINT',
        '   provider ENUM(github,linkedin,local)',
        '   provider_uid VARCHAR(255)',
        '   access_token TEXT',
        '   refresh_token TEXT',
        '   password_hash VARCHAR(255)',
        '   created_at DATETIME',
    ], ORANGE)

    # experiences (C3)
    draw_table(C3, 0.930, 'experiences', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   company VARCHAR(255)',
        '   title VARCHAR(255)',
        '   description TEXT',
        '   start_date DATE',
        '   end_date DATE',
        '   current BOOLEAN',
        '   display_order INT',
    ], AMBER)

    # publish_records (C4)
    draw_table(C4, 0.930, 'publish_records', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   published_url TEXT',
        '   version INT',
        '   published_at DATETIME',
    ], PINK)

    # ── ROW 2 ─────────────────────────────────────────────────────────────────
    # portfolios is tallest in this row
    R2_TOP = 0.560

    draw_table(C1, R2_TOP, 'portfolios', [
        'PK id BIGINT',
        'FK user_id BIGINT',
        '   slug VARCHAR(255) UNIQUE',
        '   title VARCHAR(255)',
        '   tagline TEXT',
        '   theme_key VARCHAR(50)',
        '   is_published BOOLEAN',
        '   ai_enhanced_summary TEXT',
        '   ai_enhanced_at DATETIME',
        '   created_at DATETIME',
        '   updated_at DATETIME',
    ], PURPLE)

    draw_table(C2, R2_TOP, 'sections', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   title VARCHAR(255)',
        '   type ENUM',
        '   content TEXT',
        '   display_order INT',
    ], CYAN)

    draw_table(C3, R2_TOP, 'education', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   institution VARCHAR(255)',
        '   degree VARCHAR(255)',
        '   field VARCHAR(255)',
        '   start_year INT',
        '   end_year INT',
        '   display_order INT',
    ], '#EC4899')

    draw_table(C4, R2_TOP, 'resumes', [
        'PK id BIGINT',
        'FK user_id BIGINT',
        'FK portfolio_id BIGINT',
        '   title VARCHAR(200)',
        '   template_key VARCHAR(30)',
        '   full_name VARCHAR(200)',
        '   job_title VARCHAR(200)',
        '   summary TEXT',
        '   selected_project_ids JSON',
        '   created_at DATETIME',
    ], PURPLE)

    # ── ROW 3 ─────────────────────────────────────────────────────────────────
    R3_TOP = 0.190

    draw_table(C1, R3_TOP, 'projects', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   name VARCHAR(255)',
        '   description TEXT',
        '   github_url VARCHAR(500)',
        '   languages JSON',
        '   stars INT',
        '   ai_enhanced_description TEXT',
        '   display_order INT',
    ], GREEN)

    draw_table(C2, R3_TOP, 'skills', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   name VARCHAR(255)',
        '   category VARCHAR(100)',
        '   proficiency ENUM',
        '   display_order INT',
    ], LIME)

    draw_table(C3, R3_TOP, 'certifications', [
        'PK id BIGINT',
        'FK portfolio_id BIGINT',
        '   name VARCHAR(255)',
        '   issuing_organization VARCHAR',
        '   issue_date DATE',
        '   expiry_date DATE',
        '   credential_id VARCHAR',
        '   display_order INT',
    ], RED)

    draw_table(C4, R3_TOP, 'saved_resumes', [
        'PK id BIGINT',
        'FK user_id BIGINT',
        'FK portfolio_id BIGINT',
        '   title VARCHAR(200)',
        '   template_key VARCHAR(30)',
        '   pdf_data LONGBLOB',
        '   file_size_bytes BIGINT',
        '   publish_token VARCHAR(64)',
        '   published_at DATETIME',
        '   created_at DATETIME',
    ], RED)

    # ── Relationships ──────────────────────────────────────────────────────────
    # users -> auth_identities
    fk_arr(C1 + W/2, 0.895, C2 - W/2, 0.895, ORANGE)
    # users -> portfolios (down)
    fk_arr(C1, 0.930 - tbl_height(7), C1, R2_TOP, PURPLE)
    # portfolios -> sections
    fk_arr(C1 + W/2, R2_TOP - 0.080, C2 - W/2, R2_TOP - 0.044, CYAN)
    # portfolios -> projects
    fk_arr(C1, R2_TOP - tbl_height(11), C1, R3_TOP, GREEN)
    # portfolios -> skills
    fk_arr(C1 + W/2, R2_TOP - 0.100, C2 - W/2, R3_TOP - 0.040, LIME)
    # portfolios -> experiences
    fk_arr(C1 + W/2, R2_TOP - 0.060, C3 - W/2, 0.880, AMBER)
    # portfolios -> education
    fk_arr(C1 + W/2, R2_TOP - 0.080, C3 - W/2, R2_TOP - 0.080, '#EC4899')
    # portfolios -> certifications
    fk_arr(C1 + W/2, R2_TOP - 0.100, C3 - W/2, R3_TOP - 0.070, RED)
    # portfolios -> publish_records
    fk_arr(C3 + W/2, 0.880, C4 - W/2, 0.880, PINK)
    # user + portfolio -> resumes
    fk_arr(C3 + W/2, R2_TOP - 0.080, C4 - W/2, R2_TOP - 0.080, PURPLE)
    # user + portfolio -> saved_resumes
    fk_arr(C3 + W/2, R3_TOP - 0.060, C4 - W/2, R3_TOP - 0.060, RED)

    # ── Legend ─────────────────────────────────────────────────────────────────
    legend = [(BLUE,'users'), (PURPLE,'portfolios/resumes'), (GREEN,'projects'),
              (CYAN,'sections'), (AMBER,'experiences'), ('#EC4899','education'),
              (RED,'certifications/saved'), (ORANGE,'auth'), (PINK,'publish')]
    for i, (c, lbl) in enumerate(legend):
        lx = 0.03 + i * 0.106
        ax.add_patch(FancyBboxPatch((lx, 0.010), 0.012, 0.012,
            boxstyle='square,pad=0', fc=c, ec='none', zorder=5))
        ax.text(lx + 0.016, 0.016, lbl,
                va='center', fontsize=6.5, color=TEXTSUB, zorder=5)

    save(fig, '02_erd.png')


# =============================================================================
# 3. Backend Layer Architecture
# =============================================================================

def d3_backend_layers():
    fig, ax = make_fig(18, 11,
        title='OpenFolio  —  Backend Layer Architecture',
        subtitle='Spring Boot 3.4.2  |  Java 21  |  Domain-driven package structure')

    layers = [
        ('PRESENTATION  —  @RestController', 0.840, ORANGE,
         ['AuthController', 'PortfolioController', 'PortfolioItemsController',
          'ResumeController', 'ResumeItemsController', 'ExportController',
          'IngestionController', 'PublishController',
          'PreviewController', 'PublicPortfolioController', 'UserController']),
        ('BUSINESS  —  @Service + @Transactional', 0.630, BLUE,
         ['AuthService', 'PortfolioService', 'PortfolioDataLoader',
          'ResumeService', 'ExportService', 'IngestionService',
          'PublishService', 'UserService',
          'AiResumeEnhancer', 'PortfolioHtmlGenerator', 'ResumeHtmlGenerator']),
        ('DATA ACCESS  —  JpaRepository<Entity, Long>', 0.420, CYAN,
         ['UserRepo', 'AuthIdentityRepo', 'PortfolioRepo', 'SectionRepo',
          'ProjectRepo', 'SkillRepo', 'ExperienceRepo', 'EducationRepo',
          'CertificationRepo', 'ResumeRepo', 'SavedResumeRepo', 'PublishRecordRepo']),
        ('PERSISTENCE  —  @Entity + Flyway DDL', 0.210, GREEN,
         ['User', 'AuthIdentity', 'Portfolio', 'Section',
          'Project', 'Skill', 'Experience', 'Education',
          'Certification', 'Resume', 'SavedResume', 'PublishRecord']),
    ]

    band_h = 0.185

    for (layer_name, cy, color, items) in layers:
        # Band background
        ax.add_patch(FancyBboxPatch(
            (0.03, cy - band_h / 2), 0.94, band_h,
            boxstyle='round,pad=0,rounding_size=0.012',
            lw=1.5, ec=color + '55', fc=color + '0D', zorder=2))

        # Layer label on the left edge
        ax.text(0.04, cy + band_h / 2 - 0.015, layer_name,
                ha='left', va='top', fontsize=9.5,
                fontweight='bold', color=color, zorder=5)

        # Chips: 10 per row, 1-2 rows
        n = len(items)
        cols_per_row = 10
        chip_w = 0.82 / cols_per_row - 0.006
        chip_h = 0.058

        for idx, item in enumerate(items):
            row = idx // cols_per_row
            col = idx % cols_per_row
            ix = 0.065 + col * (chip_w + 0.006) + chip_w / 2
            iy = cy + 0.022 - row * (chip_h + 0.010) - chip_h / 2
            rbox(ax, ix, iy, chip_w, chip_h, SURF2, item,
                 fs=6.8, border=color + '44', bold=False)

    # Inter-layer arrows
    for y in [0.746, 0.537, 0.328]:
        arr(ax, 0.50, y, 0.50, y - 0.040, c=TEXTMUT, lw=2.2,
            lbl='delegates', lfs=7.5, head=12)

    # Cross-cutting concerns
    ax.add_patch(FancyBboxPatch(
        (0.03, 0.035), 0.94, 0.055,
        boxstyle='round,pad=0,rounding_size=0.010',
        lw=1.2, ec=PURPLE + '55', fc=PURPLE + '0C', zorder=2))
    ax.text(0.50, 0.063,
            'Cross-Cutting:  JwtTokenProvider  |  JwtAuthFilter  |  SecurityConfig  |  '
            'GlobalExceptionHandler  |  ApiResponse<T>  |  StringListConverter',
            ha='center', va='center', fontsize=8.5, color=PURPLE, zorder=5)

    save(fig, '03_backend_layers.png')


# =============================================================================
# 4. REST API Endpoints Reference
# =============================================================================

def d4_api_endpoints():
    fig, ax = make_fig(22, 14,
        title='OpenFolio  —  REST API Reference',
        subtitle='Base: /api/v1   |   Auth: Authorization: Bearer <JWT>   |   Response: ApiResponse<T>')

    # ── Two-column layout ─────────────────────────────────────────────────────
    # Left col: AUTH, USERS, PORTFOLIOS, PORTFOLIO ITEMS, INGESTION, PUBLIC
    # Right col: RESUME BUILDER, EXPORT+PDF, SAVED RESUMES

    left_groups = [
        ('AUTH   /api/v1/auth/**', ORANGE, [
            ('POST',   '/auth/register',         'Register (email+password)  ->  TokenResponse',  False),
            ('POST',   '/auth/login',            'Login  ->  TokenResponse',                      False),
            ('POST',   '/auth/refresh',          'Rotate refresh token  ->  new TokenResponse',   False),
            ('POST',   '/auth/logout',           'Invalidate refresh token',                      False),
            ('GET',    '/auth/oauth/{provider}', 'OAuth callback  (github / linkedin)',           False),
        ]),
        ('USERS   /api/v1/users/**', BLUE, [
            ('GET',    '/users/me', 'Current user profile',      True),
            ('PUT',    '/users/me', 'Update profile fields',     True),
            ('DELETE', '/users/me', 'Delete account + all data', True),
        ]),
        ('PORTFOLIOS   /api/v1/portfolios/**', PURPLE, [
            ('GET',    '/portfolios',              'List portfolios for current user',           True),
            ('POST',   '/portfolios',              'Create portfolio',                           True),
            ('PATCH',  '/portfolios/{id}',         'Update title / tagline / theme',            True),
            ('DELETE', '/portfolios/{id}',         'Delete portfolio (cascades children)',       True),
            ('GET',    '/portfolios/{id}/preview', 'Render as HTML for WebView',                True),
            ('POST',   '/portfolios/{id}/publish', 'Publish  ->  public slug URL',              True),
            ('DELETE', '/portfolios/{id}/publish', 'Unpublish portfolio',                       True),
        ]),
        ('PORTFOLIO ITEMS   /api/v1/portfolios/{id}/**', CYAN, [
            ('CRUD', '/portfolios/{id}/projects',       'Projects (GitHub repos)',    True),
            ('CRUD', '/portfolios/{id}/skills',         'Skills + proficiency level', True),
            ('CRUD', '/portfolios/{id}/experiences',    'Work experience records',    True),
            ('CRUD', '/portfolios/{id}/education',      'Education records',          True),
            ('CRUD', '/portfolios/{id}/certifications', 'Certifications',             True),
            ('GET/PUT', '/portfolios/{id}/sections',    'List / reorder sections',   True),
        ]),
        ('INGESTION   /api/v1/ingestion/**', LIME, [
            ('POST', '/ingestion/github', 'Import GitHub profile + repos -> portfolio', True),
        ]),
        ('PUBLIC   /api/v1/public/**  (no auth)', PINK, [
            ('GET', '/public/{slug}',         'Rendered HTML portfolio page',   False),
            ('GET', '/public/{slug}/meta',    'Portfolio metadata (OG tags)',   False),
            ('GET', '/public/resume/{token}', 'Shared resume PDF (inline)',     False),
        ]),
    ]

    right_groups = [
        ('RESUME BUILDER   /api/v1/resumes/**', GREEN, [
            ('GET/POST',         '/resumes',                       'List / create resumes',                True),
            ('GET/PATCH/DELETE', '/resumes/{id}',                  'Get / update / delete resume',         True),
            ('GET',              '/resumes/{id}/preview',          'Render resume HTML',                   True),
            ('GET',              '/resumes/{id}/preview/{tmpl}',   'Preview with specific template',       True),
            ('POST',             '/resumes/{id}/pdf',              'Generate PDF  ->  download token',     True),
            ('GET',              '/resumes/{id}/pdf/inline',       'Generate PDF  ->  base64 JSON',        True),
            ('GET',              '/resumes/templates',             'List available templates',             True),
        ]),
        ('EXPORT + PDF   /api/v1/portfolios/{id}/export/**', RED, [
            ('POST', '/{id}/export/pdf',              'Generate portfolio PDF  ->  token',    True),
            ('GET',  '/{id}/export/preview',          'Preview exact PDF layout as HTML',     True),
            ('POST', '/{id}/export/pdf/inline',       'PDF as base64 for in-app viewer',     True),
            ('GET',  '/{id}/export/ai-status',        'Check if AI cache is warm',            True),
            ('POST', '/{id}/export/warm-ai',          'Pre-warm AI cache (async)',             True),
            ('POST', '/{id}/export/save',             'Generate + persist PDF to DB',         True),
            ('GET',  '/export/download/{token}',      'Download PDF (no auth, 10-min TTL)',   False),
        ]),
        ('SAVED RESUMES   /api/v1/saved-resumes/**', AMBER, [
            ('GET',    '/saved-resumes',                'List saved resumes (metadata)',       True),
            ('GET',    '/saved-resumes/{id}/pdf',       'Download saved PDF',                  True),
            ('GET',    '/saved-resumes/{id}/base64',    'Get saved PDF as base64',             True),
            ('DELETE', '/saved-resumes/{id}',           'Delete saved resume',                 True),
            ('POST',   '/saved-resumes/{id}/publish',   'Publish  ->  shareable URL',          True),
            ('DELETE', '/saved-resumes/{id}/publish',   'Unpublish  ->  remove public link',   True),
        ]),
    ]

    ROW_H = 0.021
    HDR_H = 0.027
    GAP   = 0.007

    method_colors = {
        'GET': '#22C55E', 'POST': '#3B82F6', 'PATCH': '#F59E0B',
        'PUT': '#F97316', 'DELETE': '#EF4444', 'CRUD': '#22D3EE',
        'GET/POST': '#22D3EE', 'GET/PATCH/DELETE': '#EC4899',
        'GET/PUT': '#22D3EE', 'GET/POST/PUT/DELETE': '#22D3EE',
    }

    def render_column(groups, col_x, col_w):
        y = 0.935
        for grp_title, color, endpoints in groups:
            total_h = HDR_H + len(endpoints) * ROW_H + 0.004
            if y - total_h < 0.030:
                break

            # Group header bar
            ax.add_patch(FancyBboxPatch(
                (col_x, y - HDR_H), col_w, HDR_H,
                boxstyle='round,pad=0,rounding_size=0.004',
                lw=0, fc=color, zorder=4))
            ax.text(col_x + 0.007, y - HDR_H / 2, grp_title,
                    ha='left', va='center', fontsize=8.5, fontweight='bold',
                    color='#fff', zorder=5)
            y -= HDR_H

            bw = 0.068  # method badge width
            for method, path, desc, auth in endpoints:
                row_y = y - ROW_H / 2
                mc = method_colors.get(method, TEXTSUB)

                # Method badge
                ax.add_patch(FancyBboxPatch(
                    (col_x + 0.005, row_y - ROW_H * 0.42), bw, ROW_H * 0.84,
                    boxstyle='round,pad=0,rounding_size=0.003',
                    lw=0, fc=mc + '28', zorder=4))
                ax.text(col_x + 0.005 + bw / 2, row_y, method,
                        ha='center', va='center',
                        fontsize=5.8, fontweight='bold', color=mc, zorder=5)

                # Path (monospace)
                ax.text(col_x + 0.080, row_y, path,
                        ha='left', va='center',
                        fontsize=6.5, color=TEXT, family='monospace', zorder=5)

                # Description
                ax.text(col_x + 0.230, row_y, desc[:52],
                        ha='left', va='center',
                        fontsize=7, color=TEXTSUB, zorder=5)

                # Auth badge
                auth_c, auth_t = (AMBER, 'JWT') if auth else (GREEN, 'Public')
                ax.text(col_x + col_w - 0.005, row_y, auth_t,
                        ha='right', va='center',
                        fontsize=6.5, color=auth_c, fontweight='bold', zorder=5)

                # Row separator
                ax.plot([col_x + 0.003, col_x + col_w - 0.003],
                        [y - ROW_H, y - ROW_H],
                        color=BORDER, lw=0.4, zorder=3)
                y -= ROW_H

            y -= GAP

    COL_W = 0.478
    render_column(left_groups,  0.010, COL_W)
    render_column(right_groups, 0.512, COL_W)

    # Centre divider
    ax.plot([0.500, 0.500], [0.060, 0.940], color=BORDER, lw=1.0, linestyle='--', zorder=3)

    save(fig, '04_api_endpoints.png')


# =============================================================================
# 5. Mobile Navigation Flow
# =============================================================================

def d5_mobile_navigation():
    fig, ax = make_fig(18, 11,
        title='OpenFolio  —  Mobile App Navigation',
        subtitle='React Navigation 7  |  RootNavigator gates auth state  |  Zustand useAuthStore')

    def scr(cx, cy, name, sub='', color=BLUE, w=0.155, h=0.068):
        rbox(ax, cx, cy, w, h, color + '28', name, sub,
             fs=9, sfs=7, border=color + '66')

    # App launch
    rbox(ax, 0.50, 0.895, 0.200, 0.058, PURPLE + '28',
         'App Launch', 'App.tsx  |  restoreSession()',
         fs=10, sfs=7.5, border=PURPLE + '77')

    arr(ax, 0.50, 0.866, 0.50, 0.814, c=PURPLE, lw=1.8)

    # Auth gate
    diamond(ax, 0.50, 0.778, 0.220, 0.068, PURPLE, 'isAuthenticated?', fs=9)

    # Auth branch (left)
    group_box(ax, 0.03, 0.530, 0.265, 0.215, ORANGE, 'AuthNavigator', fs=9.5)
    scr(0.163, 0.645, 'WelcomeScreen', 'GitHub OAuth via WebView', ORANGE, w=0.220)
    arr(ax, 0.388, 0.778, 0.268, 0.714, c=ORANGE, lw=1.8, lbl='not authed', lfs=7.5)

    # App branch (right)
    group_box(ax, 0.31, 0.040, 0.670, 0.695, BLUE, 'AppNavigator (Stack)', fs=9.5)

    scr(0.500, 0.640, 'DashboardScreen', 'Portfolio list  |  auto-import trigger', BLUE, w=0.230)
    arr(ax, 0.612, 0.778, 0.500, 0.674, c=BLUE, lw=1.8, lbl='authed', lfs=7.5)
    arr(ax, 0.240, 0.645, 0.384, 0.640, c=GREEN, lw=1.8, lbl='login()', lfs=7.5)

    scr(0.500, 0.500, 'EditorScreen', 'portfolioId param  |  Projects + Skills tabs', BLUE, w=0.240)
    arr(ax, 0.500, 0.606, 0.500, 0.534, c=BLUE, lw=1.5, lbl='navigate(Editor)', lfs=7)

    # 4 sub-screens on one row
    sub_y = 0.340
    sub_items = [
        (0.370, 'PreviewScreen',      'WebView + theme switcher', CYAN),
        (0.505, 'PublishScreen',      'QR code + share + link',   GREEN),
        (0.635, 'ExportScreen',       'PDF templates + AI toggle', RED),
        (0.770, 'ResumeBuilderScreen','Create / manage resumes',   PURPLE),
    ]
    for cx, name, sub, color in sub_items:
        scr(cx, sub_y, name, sub, color, w=0.125)
        arr(ax, 0.500, 0.466, cx, sub_y + 0.034, c=TEXTMUT, lw=1.3)

    # Resume sub-screens
    res_y = 0.175
    scr(0.665, res_y, 'ResumeTemplatesScreen', 'classic/modern/minimal/bold', PURPLE, w=0.165)
    scr(0.840, res_y, 'ResumePreviewScreen',   'WebView resume HTML', PURPLE, w=0.155)
    arr(ax, 0.770, 0.306, 0.665, res_y + 0.034, c=PURPLE, lw=1.2)
    arr(ax, 0.770, 0.306, 0.840, res_y + 0.034, c=PURPLE, lw=1.2)

    # Logout arc
    ax.annotate('', xy=(0.163, 0.745), xytext=(0.384, 0.640),
                arrowprops=dict(arrowstyle='->', color=RED + 'AA', lw=1.3,
                                connectionstyle='arc3,rad=-0.35',
                                linestyle='dashed', mutation_scale=8), zorder=3)
    ax.text(0.225, 0.710, 'logout()', fontsize=7.5, color=RED + 'AA', zorder=6)

    save(fig, '05_mobile_navigation.png')


# =============================================================================
# 6. OAuth + JWT Auth Flow (Sequence)
# =============================================================================

def d6_auth_flow():
    fig, ax = make_fig(16, 11,
        title='OpenFolio  —  OAuth 2.0 + JWT Authentication Flow',
        subtitle='GitHub OAuth Authorization Code  |  Access token 15 min  |  Refresh token 30 days')

    actors = [
        (0.10, PURPLE, 'Mobile App'),
        (0.30, ORANGE, 'GitHub OAuth'),
        (0.52, BLUE,   'Spring Boot'),
        (0.73, CYAN,   'MySQL DB'),
        (0.92, GREEN,  'Keychain'),
    ]
    TOP, BOT = 0.870, 0.040

    for x, c, lbl in actors:
        seq_bar(ax, x, TOP, BOT, c, lbl)

    msgs = [
        (0.10, 0.30, 0.803, ORANGE, '1. Open GitHub OAuth URL  (client_id, scope, redirect_uri)', False),
        (0.30, 0.10, 0.740, ORANGE, '2. User approves  ->  redirect with ?code=AUTH_CODE',        False),
        (0.10, 0.52, 0.677, BLUE,   '3. POST /auth/oauth/github  { code }',                       False),
        (0.52, 0.30, 0.614, ORANGE, '4. Exchange code  ->  GitHub access_token',                  False),
        (0.30, 0.52, 0.551, ORANGE, '5. GitHub profile  { id, email, login, avatar }',            True),
        (0.52, 0.73, 0.488, CYAN,   '6. UPSERT user + auth_identity',                             False),
        (0.73, 0.52, 0.425, CYAN,   '7. Return { id, email }',                                    True),
        (0.52, 0.10, 0.362, BLUE,   '8. TokenResponse { accessToken (15min), refreshToken (30d) }', True),
        (0.10, 0.92, 0.299, GREEN,  '9. Save tokens to Keychain  (react-native-keychain)',         False),
    ]

    for x1, x2, y, c, lbl, dashed in msgs:
        seq_msg(ax, x1, x2, y, c, lbl, dashed=dashed)

    # Divider
    ax.plot([0.04, 0.96], [0.260, 0.260], color=TEXTMUT, lw=1.0, linestyle='--')
    ax.text(0.50, 0.250, 'Token Refresh Flow (on 401)', ha='center', va='top',
            fontsize=8.5, color=TEXTMUT, style='italic')

    refresh_msgs = [
        (0.10, 0.52, 0.205, RED,  '10. Any API call returns 401 Unauthorized',       False),
        (0.10, 0.52, 0.155, BLUE, '11. POST /auth/refresh  { refreshToken }',        False),
        (0.52, 0.10, 0.105, BLUE, '12. New accessToken + refreshToken (rotation)',   True),
        (0.10, 0.92, 0.055, GREEN,'13. Save new tokens to Keychain',                 False),
    ]
    for x1, x2, y, c, lbl, dashed in refresh_msgs:
        seq_msg(ax, x1, x2, y, c, lbl, dashed=dashed)

    save(fig, '06_auth_flow.png')


# =============================================================================
# 7. GitHub Ingestion + AI Pipeline
# =============================================================================

def d7_ingestion_pipeline():
    fig, ax = make_fig(18, 11,
        title='OpenFolio  —  GitHub Ingestion Pipeline',
        subtitle='POST /api/v1/ingestion/github  |  Async AI enhancement via Ollama qwen2.5:14b')

    def step(cx, cy, lbl, sub='', color=BLUE, w=0.190, h=0.068):
        rbox(ax, cx, cy, w, h, color + '28', lbl, sub,
             fs=9, sfs=7, border=color + '66')

    # ── Main pipeline (left) ──────────────────────────────────────────────────
    LEFT_X = 0.195
    pipeline = [
        (LEFT_X, 0.875, 'POST /ingestion/github',    '{ githubUsername }',                        PURPLE),
        (LEFT_X, 0.790, 'Resolve GitHub Token',      'OAuth token or GITHUB_TOKEN env var',       ORANGE),
        (LEFT_X, 0.705, 'Fetch GitHub Profile',      'GET /users/{username}',                     AMBER),
        (LEFT_X, 0.620, 'Fetch All Repos',           'GET /users/{u}/repos  (max 100)',           AMBER),
        (LEFT_X, 0.535, 'Fetch Profile README',      'GET /repos/{u}/{u}/readme',                 AMBER),
        (LEFT_X, 0.450, 'Filter & Sort Repos',       'Non-fork, non-archived  |  by stars',       CYAN),
        (LEFT_X, 0.365, 'Parallel Language Fetch',   'Top 30 repos  via CompletableFuture.allOf()', GREEN),
        (LEFT_X, 0.280, 'Build Skills from Languages','Bytes -> proficiency level mapping',        LIME),
        (LEFT_X, 0.190, 'Persist Portfolio',         'Projects + Skills + Sections -> DB',         BLUE),
    ]

    for cx, cy, lbl, sub, color in pipeline:
        step(cx, cy, lbl, sub, color)

    for i in range(len(pipeline) - 1):
        cy1 = pipeline[i][1] - 0.034
        cy2 = pipeline[i + 1][1] + 0.034
        arr(ax, LEFT_X, cy1, LEFT_X, cy2, c=TEXTMUT, lw=1.5, head=9)

    # ── Re-import strategy box ────────────────────────────────────────────────
    ri_x, ri_y, ri_w, ri_h = 0.415, 0.620, 0.270, 0.235
    ax.add_patch(FancyBboxPatch(
        (ri_x, ri_y), ri_w, ri_h,
        boxstyle='round,pad=0,rounding_size=0.012',
        lw=1.5, ec=PURPLE + '55', fc=PURPLE + '0A', zorder=2))
    ax.text(ri_x + ri_w / 2, ri_y + ri_h - 0.014,
            'RE-IMPORT STRATEGY', ha='center', va='top',
            fontsize=9.5, fontweight='bold', color=PURPLE, zorder=5)
    lines = [
        ('IF portfolio exists:', PURPLE),
        ('  Keep portfolio row + user edits', TEXT),
        ('  Keep experience / education / certs', TEXT),
        ('  Clear projects, skills, sections', TEXT),
        ('  Update title + tagline from GitHub', TEXT),
        ('IF no portfolio:', PURPLE),
        ('  Create portfolio + 6 default sections', TEXT),
    ]
    for i, (txt, tc) in enumerate(lines):
        ax.text(ri_x + 0.012, ri_y + ri_h - 0.052 - i * 0.026,
                txt, ha='left', va='center', fontsize=8, color=tc, zorder=5)

    arr(ax, LEFT_X + 0.095, 0.365, ri_x, 0.720, c=PURPLE, lw=1.3,
        lbl='check DB', lfs=7.5, rad=-0.2)

    # ── AI Enhancement (right) ────────────────────────────────────────────────
    group_box(ax, 0.640, 0.065, 0.345, 0.850, GREEN, 'AI ENHANCEMENT  (async)', fs=10)

    rbox(ax, 0.815, 0.845, 0.315, 0.060, GREEN + '28',
         'CompletableFuture.allOf()  (fire-and-forget)',
         'Returns HTTP 200 immediately  |  AI runs in background',
         fs=8.5, sfs=7, border=GREEN + '66')

    rbox(ax, 0.720, 0.740, 0.140, 0.080, AMBER + '28',
         'Summary\nEnhancement',
         'enhanceProfessionalSummary()\n60-100 words, 3rd person',
         fs=8, sfs=6.5, border=AMBER + '66')

    rbox(ax, 0.902, 0.740, 0.140, 0.080, GREEN + '28',
         'Project Desc.\nEnhancement',
         'enhanceProjectDescription()\n3-5 bullet points, top 5 repos',
         fs=8, sfs=6.5, border=GREEN + '66')

    rbox(ax, 0.815, 0.620, 0.240, 0.068, CYAN + '28',
         'OllamaClient.chat()',
         'POST localhost:11434/api/chat  |  qwen2.5:14b',
         fs=8.5, sfs=7, border=CYAN + '66')

    rbox(ax, 0.815, 0.510, 0.265, 0.070, LIME + '28',
         'DB Cache Check',
         'ai_enhanced_description / ai_enhanced_summary\nIf cached -> skip AI call entirely',
         fs=8.5, sfs=7, border=LIME + '66')

    rbox(ax, 0.815, 0.395, 0.240, 0.068, BLUE + '28',
         'Persist to DB',
         'projectRepository.save()  |  portfolioRepository.save()',
         fs=8.5, sfs=7, border=BLUE + '66')

    rbox(ax, 0.815, 0.285, 0.295, 0.068, SURF2,
         'Client polls GET /export/ai-status',
         'Returns { ready: true } when all cached',
         fs=8.5, sfs=7, border=TEXTMUT)

    rbox(ax, 0.815, 0.155, 0.295, 0.068, SURF2,
         'POST /export/warm-ai  triggers same flow',
         'Proactive cache fill before PDF export',
         fs=8.5, sfs=7, border=TEXTMUT)

    arr(ax, 0.815, 0.815, 0.720, 0.780, c=AMBER, lw=1.3)
    arr(ax, 0.815, 0.815, 0.902, 0.780, c=GREEN, lw=1.3)
    arr(ax, 0.720, 0.700, 0.815, 0.654, c=CYAN, lw=1.3)
    arr(ax, 0.902, 0.700, 0.815, 0.654, c=CYAN, lw=1.3)
    arr(ax, 0.815, 0.586, 0.815, 0.545, c=LIME, lw=1.3)
    arr(ax, 0.815, 0.475, 0.815, 0.429, c=BLUE, lw=1.3)
    arr(ax, 0.815, 0.361, 0.815, 0.319, c=TEXTMUT, lw=1.3)

    # Main pipeline -> AI
    arr(ax, LEFT_X + 0.095, 0.190, 0.640, 0.845,
        c=GREEN, lw=1.8, lbl='async AI trigger', lfs=7.5, rad=-0.15)

    save(fig, '07_ingestion_pipeline.png')


# =============================================================================
# 8. PDF Export Pipeline
# =============================================================================

def d8_pdf_export():
    fig, ax = make_fig(17, 10,
        title='OpenFolio  —  PDF Export Pipeline',
        subtitle='Portfolio PDF  (ExportService)  and  Resume PDF  (ResumeService)  |  openhtmltopdf rendering')

    def box(cx, cy, lbl, sub='', color=BLUE, w=0.170, h=0.075):
        rbox(ax, cx, cy, w, h, color + '28', lbl, sub,
             fs=9, sfs=7, border=color + '55')

    # ── PORTFOLIO PATH ────────────────────────────────────────────────────────
    ax.text(0.50, 0.930, 'PORTFOLIO PDF  —  POST /portfolios/{id}/export/pdf',
            ha='center', va='center', fontsize=11, fontweight='bold', color=PURPLE)

    steps_p = [
        (0.095, 0.830, 'Client Request',        'template + options',          PURPLE),
        (0.270, 0.830, 'PortfolioDataLoader',   'verify ownership / load all', BLUE),
        (0.455, 0.830, 'AI Enhancement',        'enhanceBundle() / cached DB', GREEN),
        (0.640, 0.830, 'PortfolioHtmlGenerator','XHTML + CSS 2.1 / themes',    AMBER),
        (0.830, 0.830, 'PdfRendererBuilder',    'openhtmltopdf / byte[]',      RED),
    ]
    for cx, cy, lbl, sub, c in steps_p:
        box(cx, cy, lbl, sub, c)
    for i in range(len(steps_p) - 1):
        x1 = steps_p[i][0] + 0.085
        x2 = steps_p[i + 1][0] - 0.085
        arr(ax, x1, 0.830, x2, 0.830, c=TEXTMUT, lw=1.5, head=9)

    # PDF bytes node
    rbox(ax, 0.830, 0.700, 0.170, 0.058, RED + '28',
         'byte[] pdfBytes', 'raw PDF in memory',
         fs=8.5, sfs=7, border=RED + '55')
    arr(ax, 0.830, 0.792, 0.830, 0.729, c=RED, lw=1.5)

    # Three branches
    branches = [
        (0.250, 0.565, 'base64 encode',   'JSON { base64 } / in-app viewer',  CYAN),
        (0.530, 0.565, 'ExportTempStore', 'UUID token / 10-min TTL -> URL',   AMBER),
        (0.810, 0.565, 'SavedResume DB',  'LONGBLOB / Publish token opt.',    BLUE),
    ]
    for cx, cy, lbl, sub, c in branches:
        box(cx, cy, lbl, sub, c)
        arr(ax, 0.830, 0.671, cx, cy + 0.038, c=RED, lw=1.2)

    # Branch continuations
    rbox(ax, 0.530, 0.445, 0.210, 0.055, AMBER + '18',
         'GET /export/download/{token}',
         'Unauthenticated  |  Content-Disposition: attachment',
         fs=7.5, sfs=6.5, border=AMBER + '44')
    arr(ax, 0.530, 0.535, 0.530, 0.473, c=AMBER, lw=1.2)

    rbox(ax, 0.810, 0.445, 0.210, 0.055, PINK + '18',
         'POST /saved-resumes/{id}/publish',
         'Sets publish_token  |  Public shareable URL',
         fs=7.5, sfs=6.5, border=PINK + '44')
    arr(ax, 0.810, 0.535, 0.810, 0.473, c=BLUE, lw=1.2)

    # ── RESUME PATH ───────────────────────────────────────────────────────────
    ax.text(0.50, 0.365, 'RESUME PDF  —  POST /resumes/{id}/pdf  |  GET /resumes/{id}/pdf/inline',
            ha='center', va='center', fontsize=11, fontweight='bold', color=GREEN)

    steps_r = [
        (0.095, 0.270, 'Resume Entity',      'filtered by selected IDs',        GREEN),
        (0.270, 0.270, 'ResumeBundle',       'projects/skills/exp/edu',         CYAN),
        (0.455, 0.270, 'ResumeHtmlGenerator','classic/modern/minimal/bold',     AMBER),
        (0.640, 0.270, 'PdfRendererBuilder', 'openhtmltopdf / byte[]',          RED),
        (0.830, 0.270, 'Token / base64',     'TempStore / inline JSON',         BLUE),
    ]
    for cx, cy, lbl, sub, c in steps_r:
        box(cx, cy, lbl, sub, c)
    for i in range(len(steps_r) - 1):
        x1 = steps_r[i][0] + 0.085
        x2 = steps_r[i + 1][0] - 0.085
        arr(ax, x1, 0.270, x2, 0.270, c=TEXTMUT, lw=1.5, head=9)

    # Template chips
    templates = [
        (0.160, 0.120, 'Classic',  '#7C3AED'),
        (0.340, 0.120, 'Modern',   '#2563EB'),
        (0.520, 0.120, 'Minimal',  '#0D9488'),
        (0.700, 0.120, 'Bold',     '#DC2626'),
    ]
    for cx, cy, name, color in templates:
        rbox(ax, cx, cy, 0.145, 0.050, color + '22', name,
             fs=9.5, border=color + '66', bold=True, tc=color)
        arr(ax, cx, cy + 0.025, 0.455, 0.232, c=color + '66', lw=1.0, head=7)

    save(fig, '08_pdf_export_flow.png')


# =============================================================================
# 9. Portfolio & Resume Lifecycle
# =============================================================================

def d9_portfolio_lifecycle():
    fig, ax = make_fig(16, 11,
        title='OpenFolio  —  Portfolio & Resume Lifecycle',
        subtitle='State machines: portfolio publish cycle  |  resume create -> generate -> save -> share')

    def state(cx, cy, lbl, sub='', color=BLUE, r=0.072):
        ax.add_patch(plt.Circle((cx, cy), r,
                                facecolor=color + '25',
                                edgecolor=color, linewidth=2, zorder=4))
        dy = 0.014 if sub else 0
        ax.text(cx, cy + dy, lbl, ha='center', va='center',
                fontsize=9.5, fontweight='bold', color=color, zorder=5)
        if sub:
            ax.text(cx, cy - 0.022, sub, ha='center', va='center',
                    fontsize=7, color=color, alpha=0.8, zorder=5)

    def trans(x1, y1, x2, y2, lbl, color, rad=0.0):
        arr(ax, x1, y1, x2, y2, c=color, lw=2.0, lbl=lbl, lfs=8, rad=rad, head=12)

    # ── Portfolio states ──────────────────────────────────────────────────────
    ax.text(0.50, 0.905, 'PORTFOLIO  STATES', ha='center', va='center',
            fontsize=11, fontweight='bold', color=PURPLE)

    state(0.130, 0.830, 'IMPORT',    'POST /ingestion/github',   GREEN,  r=0.072)
    state(0.330, 0.830, 'DRAFT',     'is_published = false',     BLUE,   r=0.072)
    state(0.550, 0.830, 'PUBLISHED', 'is_published = true',      GREEN,  r=0.072)
    state(0.760, 0.830, 'UPDATED',   'AI enhanced / re-imported', AMBER, r=0.072)

    trans(0.203, 0.830, 0.257, 0.830, 'create portfolio', GREEN)
    trans(0.403, 0.830, 0.477, 0.830, 'POST /publish',    BLUE)
    trans(0.624, 0.830, 0.687, 0.830, 'POST /ingestion',  AMBER)
    trans(0.688, 0.768, 0.402, 0.768, 'DELETE /publish',  RED, rad=0.15)
    trans(0.330, 0.758, 0.130, 0.758, 'DELETE portfolio', RED + '88', rad=0.2)

    # ── Resume states ─────────────────────────────────────────────────────────
    ax.text(0.50, 0.640, 'RESUME  STATES', ha='center', va='center',
            fontsize=11, fontweight='bold', color=BLUE)

    state(0.110, 0.530, 'CREATE',       'POST /resumes',         BLUE,  r=0.062)
    state(0.280, 0.530, 'EDIT',         'PATCH /resumes/{id}',   CYAN,  r=0.062)
    state(0.450, 0.530, 'PREVIEW',      'GET /{id}/preview',     AMBER, r=0.062)
    state(0.620, 0.530, 'GENERATE PDF', 'POST /pdf / inline',    RED,   r=0.062)
    state(0.800, 0.530, 'SAVED RESUME', 'POST /export/save',     GREEN, r=0.062)

    trans(0.172, 0.530, 0.218, 0.530, 'fill / template', CYAN)
    trans(0.342, 0.530, 0.388, 0.530, 'render', AMBER)
    trans(0.512, 0.530, 0.558, 0.530, 'generate', RED)
    trans(0.682, 0.530, 0.738, 0.530, 'save', GREEN)

    # Saved -> Published -> Unpublished
    state(0.800, 0.360, 'PUBLISHED RESUME', 'POST /publish -> URL', PINK, r=0.062)
    trans(0.800, 0.468, 0.800, 0.422, 'POST /publish', PINK)

    state(0.800, 0.200, 'PRIVATE',          'token removed', TEXTMUT, r=0.055)
    trans(0.800, 0.298, 0.800, 0.255, 'DELETE /publish', RED + '88')

    # Template panel
    ax.text(0.27, 0.330, 'Resume Templates:', ha='center',
            fontsize=9.5, fontweight='bold', color=PURPLE)
    for i, (name, c) in enumerate([
        ('Classic', '#7C3AED'), ('Modern', '#2563EB'),
        ('Minimal', '#0D9488'), ('Bold', '#DC2626'),
    ]):
        cx = 0.085 + i * 0.118
        rbox(ax, cx, 0.240, 0.100, 0.050,
             c + '22', name, fs=8.5, border=c + '66', tc=c)

    # AI cache note
    rbox(ax, 0.370, 0.110, 0.600, 0.062, GREEN + '12',
         'AI Enhancement: async post-import  |  cached in DB (ai_enhanced_at)',
         'Triggered again on PDF export when aiRewrite=true param is passed',
         fs=8, sfs=7, border=GREEN + '33', bold=False)

    save(fig, '09_portfolio_lifecycle.png')


# =============================================================================
# 10. Technology Stack Overview
# =============================================================================

def d10_tech_stack():
    fig, ax = make_fig(20, 12,
        title='OpenFolio  —  Technology Stack',
        subtitle='React Native 0.84  +  Spring Boot 3.4.2  +  MySQL 8  +  Ollama AI')

    categories = [
        ('MOBILE  (React Native)', PURPLE, [
            ('React Native', '0.84', 'iOS + Android'),
            ('React', '19.2', 'UI framework'),
            ('TypeScript', '5.8', 'Type safety'),
            ('Zustand', '5.0', 'State management'),
            ('React Navigation', '7.x', 'Screen routing'),
            ('Axios', '1.13', 'HTTP client + JWT interceptors'),
            ('RN Reanimated', '4.2', 'Smooth animations'),
            ('RN Keychain', '10.0', 'Secure token storage'),
            ('RN WebView', '13.16', 'HTML portfolio preview'),
            ('RN QRCode SVG', '6.3', 'QR code generation'),
            ('RN Image Picker', '8.2', 'Profile photo import'),
            ('RN FS', '2.20', 'File system access'),
        ]),
        ('BACKEND  (Spring Boot)', BLUE, [
            ('Spring Boot', '3.4.2', 'Application framework'),
            ('Java', '21 LTS', 'Language + virtual threads'),
            ('Spring Security', '6.x', 'Auth + JWT filter chain'),
            ('Spring Data JPA', '3.4', 'ORM abstraction layer'),
            ('Hibernate', '6.x', 'JPA implementation'),
            ('JJWT', '0.12.5', 'JWT sign + verify'),
            ('Lombok', 'latest', 'Boilerplate reduction'),
            ('openhtmltopdf', '1.0.10', 'HTML -> PDF rendering'),
            ('Jackson', 'embedded', 'JSON serialization'),
            ('Maven', '3.x', 'Build + dependency tool'),
        ]),
        ('DATABASE  (MySQL + Flyway)', CYAN, [
            ('MySQL', '8.x', 'Primary relational database'),
            ('Flyway', '10.x', '17 versioned migrations'),
            ('12 JPA Entities', 'V001-V017', 'Full schema coverage'),
            ('StringListConverter', 'custom', 'JSON array columns'),
        ]),
        ('AI  (Ollama + GitHub)', GREEN, [
            ('Ollama', 'local server', 'AI inference at localhost:11434'),
            ('qwen2.5:14b', 'LLM model', 'Text generation + rewriting'),
            ('GitHub API', 'v3 REST', 'Profile, repos, languages'),
            ('GitHub OAuth', '2.0', 'Primary sign-in flow'),
            ('LinkedIn OAuth', '2.0', 'Profile import SSO'),
        ]),
        ('SECURITY', ORANGE, [
            ('JWT Access', 'HMAC-SHA', '15 minute expiry'),
            ('JWT Refresh', 'HMAC-SHA', '30 day expiry, rotated'),
            ('BCrypt', 'Spring', 'Password hashing'),
            ('Spring Security', 'stateless', 'Filter chain, no sessions'),
            ('RN Keychain', 'iOS/Android', 'Hardware-backed token store'),
        ]),
        ('TOOLING', AMBER, [
            ('Node.js', '>= 22.11', 'Mobile build runtime'),
            ('Metro', '0.84', 'React Native bundler'),
            ('Babel', '7.25', 'JavaScript transpiler'),
            ('ESLint + Prettier', 'config', 'Code quality'),
            ('CocoaPods', 'iOS', 'iOS native dependencies'),
            ('Spring Actuator', 'health', 'Monitoring endpoint'),
            ('Gradle', 'Android', 'Android build system'),
        ]),
    ]

    # 2 rows x 3 columns
    COL_W = 0.310
    ROW_H = 0.420
    START_X = [0.020, 0.344, 0.668]
    START_Y = [0.525, 0.065]

    for i, (cat_title, color, techs) in enumerate(categories):
        col = i % 3
        row = i // 3
        bx = START_X[col]
        by = START_Y[row]
        bh = ROW_H - 0.018

        # Container
        ax.add_patch(FancyBboxPatch(
            (bx, by), COL_W, bh,
            boxstyle='round,pad=0,rounding_size=0.012',
            lw=1.5, ec=color + '55', fc=color + '0A', zorder=2))

        # Header strip
        ax.add_patch(FancyBboxPatch(
            (bx, by + bh - 0.052), COL_W, 0.050,
            boxstyle='round,pad=0,rounding_size=0.010',
            lw=0, fc=color, zorder=4))
        ax.text(bx + COL_W / 2, by + bh - 0.027, cat_title,
                ha='center', va='center', fontsize=10.5,
                fontweight='bold', color='#fff', zorder=5)

        # Tech rows (text-based, no chip overlap)
        y_start = by + bh - 0.068
        for j, (name, version, desc) in enumerate(techs):
            ry = y_start - j * 0.028
            if ry < by + 0.010:
                break
            # Name
            ax.text(bx + 0.012, ry, name,
                    ha='left', va='center', fontsize=8.5,
                    fontweight='bold', color=TEXT, zorder=5)
            # Version badge
            vw = 0.058
            ax.add_patch(FancyBboxPatch(
                (bx + COL_W - vw - 0.008, ry - 0.009), vw, 0.018,
                boxstyle='round,pad=0,rounding_size=0.004',
                lw=0, fc=color + '33', zorder=4))
            ax.text(bx + COL_W - vw / 2 - 0.008, ry,
                    version[:8], ha='center', va='center',
                    fontsize=6.5, color=color, fontweight='bold', zorder=5)
            # Description
            ax.text(bx + 0.012, ry - 0.014, desc[:38],
                    ha='left', va='center', fontsize=6.5,
                    color=TEXTSUB, zorder=5)
            # Separator
            if j < len(techs) - 1:
                ax.plot([bx + 0.008, bx + COL_W - 0.008],
                        [ry - 0.022, ry - 0.022],
                        color=BORDER, lw=0.5, zorder=3)

    # Stats bar
    stats = [
        ('12', 'DB Tables',        CYAN),
        ('17', 'Migrations',       GREEN),
        ('9',  'Mobile Screens',   PURPLE),
        ('4',  'Resume Templates', AMBER),
        ('3',  'Portfolio Themes', BLUE),
        ('6',  'Domain Packages',  ORANGE),
        ('2',  'OAuth Providers',  PINK),
        ('1',  'AI Model (local)', GREEN),
    ]
    sw = 0.108
    sx0 = 0.022
    for i, (val, lbl, color) in enumerate(stats):
        cx = sx0 + i * (sw + 0.007) + sw / 2
        ax.add_patch(FancyBboxPatch(
            (cx - sw / 2, 0.012), sw, 0.044,
            boxstyle='round,pad=0,rounding_size=0.008',
            lw=1, ec=color + '55', fc=color + '15', zorder=4))
        ax.text(cx, 0.044, val, ha='center', va='center',
                fontsize=13, fontweight='bold', color=color, zorder=5)
        ax.text(cx, 0.022, lbl, ha='center', va='center',
                fontsize=7, color=TEXTSUB, zorder=5)

    save(fig, '10_tech_stack.png')


# =============================================================================
# MAIN
# =============================================================================

def main():
    print('\n  OpenFolio -- Generating architecture diagrams...\n')

    diagrams = [
        ('System Architecture (C4)',    d1_system_architecture),
        ('Entity Relationship Diagram', d2_erd),
        ('Backend Layer Architecture',  d3_backend_layers),
        ('REST API Endpoints Reference', d4_api_endpoints),
        ('Mobile Navigation Flow',      d5_mobile_navigation),
        ('OAuth + JWT Auth Flow',       d6_auth_flow),
        ('GitHub Ingestion Pipeline',   d7_ingestion_pipeline),
        ('PDF Export Pipeline',         d8_pdf_export),
        ('Portfolio & Resume Lifecycle', d9_portfolio_lifecycle),
        ('Technology Stack Overview',   d10_tech_stack),
    ]

    for i, (name, fn) in enumerate(diagrams, 1):
        print(f'  [{i:02d}/{len(diagrams)}]  {name}')
        try:
            fn()
        except Exception as e:
            print(f'           ERROR: {e}')
            import traceback
            traceback.print_exc()

    print(f'\n  All diagrams saved to: {OUTPUT}\n')


if __name__ == '__main__':
    main()
