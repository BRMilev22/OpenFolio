export type ResumeInfo = {
  id: number;
  portfolioId: number;
  title: string;
  templateKey: string;
  fullName: string | null;
  jobTitle: string | null;
  email: string | null;
  phone: string | null;
  location: string | null;
  website: string | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  summary: string | null;
  selectedProjectIds: string | null;
  selectedSkillIds: string | null;
  selectedExperienceIds: string | null;
  selectedEducationIds: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateResumeRequest = {
  portfolioId: number;
  title?: string;
};

export type UpdateResumeRequest = {
  title?: string;
  templateKey?: string;
  fullName?: string;
  jobTitle?: string;
  email?: string;
  phone?: string;
  location?: string;
  website?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  summary?: string;
  selectedProjectIds?: string;
  selectedSkillIds?: string;
  selectedExperienceIds?: string;
  selectedEducationIds?: string;
};

export type TemplateInfo = {
  key: string;
  name: string;
  description: string;
  accentColor: string;
  emoji: string;
};
