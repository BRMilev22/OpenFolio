export const endpoints = {
  auth: {
    register: '/auth/register',
    login: '/auth/login',
    refresh: '/auth/refresh',
    logout: '/auth/logout',
    oauthGitHub: '/auth/oauth/github',
    oauthLinkedIn: '/auth/oauth/linkedin',
  },
  users: {
    me: '/users/me',
  },
  portfolios: {
    list: '/portfolios',
    detail: (id: number) => `/portfolios/${id}`,
    publish: (id: number) => `/portfolios/${id}/publish`,
    publishStatus: (id: number) => `/portfolios/${id}/publish/status`,
    preview: (id: number) => `/portfolios/${id}/preview`,
    sections: (id: number) => `/portfolios/${id}/sections`,
    reorder: (id: number) => `/portfolios/${id}/sections/reorder`,
    projects: (id: number) => `/portfolios/${id}/projects`,
    skills: (id: number) => `/portfolios/${id}/skills`,
    experiences: (id: number) => `/portfolios/${id}/experiences`,
    education: (id: number) => `/portfolios/${id}/education`,
    certifications: (id: number) => `/portfolios/${id}/certifications`,
    exportPdf: (id: number) => `/portfolios/${id}/export/pdf`,
    exportPreview: (id: number) => `/portfolios/${id}/export/preview`,
    exportPdfInline: (id: number) => `/portfolios/${id}/export/pdf/inline`,
    aiStatus: (id: number) => `/portfolios/${id}/export/ai-status`,
    warmAi: (id: number) => `/portfolios/${id}/export/warm-ai`,
  },
  sections: {
    detail: (id: number) => `/sections/${id}`,
  },
  projects: {
    detail: (id: number) => `/projects/${id}`,
  },
  skills: {
    detail: (id: number) => `/skills/${id}`,
  },
  experiences: {
    detail: (id: number) => `/experiences/${id}`,
  },
  education: {
    detail: (id: number) => `/education/${id}`,
  },
  certifications: {
    detail: (id: number) => `/certifications/${id}`,
  },
  ingestion: {
    github: '/ingestion/github',
    linkedin: '/ingestion/linkedin',
  },
  resumes: {
    list: '/resumes',
    detail: (id: number) => `/resumes/${id}`,
    preview: (id: number) => `/resumes/${id}/preview`,
    previewWithTemplate: (id: number, templateKey: string) =>
      `/resumes/${id}/preview/${templateKey}`,
    pdf: (id: number) => `/resumes/${id}/pdf`,
    pdfInline: (id: number) => `/resumes/${id}/pdf/inline`,
    templates: '/resumes/templates',
  },
  export: {
    download: (token: string) => `/export/download/${token}`,
  },
  savedResumes: {
    list: '/saved-resumes',
    pdf: (id: number) => `/saved-resumes/${id}/pdf`,
    base64: (id: number) => `/saved-resumes/${id}/base64`,
    delete: (id: number) => `/saved-resumes/${id}`,
    save: (portfolioId: number) => `/portfolios/${portfolioId}/export/save`,
    publish: (id: number) => `/saved-resumes/${id}/publish`,
  },
  public: {
    portfolio: (slug: string) => `/public/${slug}`,
    portfolioMeta: (slug: string) => `/public/${slug}/meta`,
  },
} as const;
