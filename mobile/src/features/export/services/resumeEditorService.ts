import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {ApiResponse} from '../../../api/types/common';

// ─── Types ──────────────────────────────────────────────────────────────────

export interface ExperienceItem {
  id: number;
  company: string;
  title: string;
  description?: string;
  startDate?: string; // ISO date
  endDate?: string;
  current: boolean;
  displayOrder: number;
}

export interface EducationItem {
  id: number;
  institution: string;
  degree?: string;
  field?: string;
  startYear?: number;
  endYear?: number;
  displayOrder: number;
}

export interface CertificationItem {
  id: number;
  name: string;
  issuingOrganization?: string;
  issueDate?: string; // ISO date
  expiryDate?: string;
  credentialId?: string;
  credentialUrl?: string;
  displayOrder: number;
}

export type ExperienceInput = Omit<ExperienceItem, 'id'>;
export type EducationInput = Omit<EducationItem, 'id'>;
export type CertificationInput = Omit<CertificationItem, 'id'>;

// ─── Service ────────────────────────────────────────────────────────────────

export const resumeEditorService = {
  // ── Experience ───────────────────────────────────────────────────────────

  async listExperiences(portfolioId: number): Promise<ExperienceItem[]> {
    const resp = await apiClient.get<ApiResponse<ExperienceItem[]>>(
      endpoints.portfolios.experiences(portfolioId),
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data ?? [];
  },

  async createExperience(
    portfolioId: number,
    input: ExperienceInput,
  ): Promise<ExperienceItem> {
    const resp = await apiClient.post<ApiResponse<ExperienceItem>>(
      endpoints.portfolios.experiences(portfolioId),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async updateExperience(
    id: number,
    input: Partial<ExperienceInput>,
  ): Promise<ExperienceItem> {
    const resp = await apiClient.put<ApiResponse<ExperienceItem>>(
      endpoints.experiences.detail(id),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async deleteExperience(id: number): Promise<void> {
    await apiClient.delete(endpoints.experiences.detail(id), {timeout: 30_000});
  },

  // ── Education ────────────────────────────────────────────────────────────

  async listEducation(portfolioId: number): Promise<EducationItem[]> {
    const resp = await apiClient.get<ApiResponse<EducationItem[]>>(
      endpoints.portfolios.education(portfolioId),
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data ?? [];
  },

  async createEducation(
    portfolioId: number,
    input: EducationInput,
  ): Promise<EducationItem> {
    const resp = await apiClient.post<ApiResponse<EducationItem>>(
      endpoints.portfolios.education(portfolioId),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async updateEducation(
    id: number,
    input: Partial<EducationInput>,
  ): Promise<EducationItem> {
    const resp = await apiClient.put<ApiResponse<EducationItem>>(
      endpoints.education.detail(id),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async deleteEducation(id: number): Promise<void> {
    await apiClient.delete(endpoints.education.detail(id), {timeout: 30_000});
  },

  // ── Certifications ───────────────────────────────────────────────────────

  async listCertifications(portfolioId: number): Promise<CertificationItem[]> {
    const resp = await apiClient.get<ApiResponse<CertificationItem[]>>(
      endpoints.portfolios.certifications(portfolioId),
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data ?? [];
  },

  async createCertification(
    portfolioId: number,
    input: CertificationInput,
  ): Promise<CertificationItem> {
    const resp = await apiClient.post<ApiResponse<CertificationItem>>(
      endpoints.portfolios.certifications(portfolioId),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async updateCertification(
    id: number,
    input: Partial<CertificationInput>,
  ): Promise<CertificationItem> {
    const resp = await apiClient.put<ApiResponse<CertificationItem>>(
      endpoints.certifications.detail(id),
      input,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async deleteCertification(id: number): Promise<void> {
    await apiClient.delete(endpoints.certifications.detail(id), {timeout: 30_000});
  },
};
