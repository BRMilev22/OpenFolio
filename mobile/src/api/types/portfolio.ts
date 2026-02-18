export type Portfolio = {
  id: number;
  slug: string;
  title: string;
  tagline: string | null;
  published: boolean;
  themeKey: string;
  projectCount: number;
  skillCount: number;
  createdAt: string;
  updatedAt: string;
};

export type CreatePortfolioRequest = {
  title: string;
  tagline?: string;
};

export type UpdatePortfolioRequest = {
  title?: string;
  tagline?: string;
  themeKey?: string;
  published?: boolean;
};

export type IngestionRequest = {
  githubUsername: string;
};

export type PublishResponse = {
  portfolioId: number;
  slug: string;
  publicUrl: string;
  version: number;
  publishedAt: string;
};

export type ExportResponse = {
  token: string;
  downloadUrl: string;
  template: string;
};

