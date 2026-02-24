import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {ExportResponse} from '../../../api/types/portfolio';
import type {ApiResponse} from '../../../api/types/common';

export interface SavedResumeInfo {
  id: number;
  portfolioId: number;
  title: string;
  templateKey: string;
  fileSizeBytes: number;
  createdAt: string;
  publicUrl: string | null;
}

export interface ResumeOptions {
  template: 'pdf' | 'dark' | 'minimal' | 'hacker';
  aiRewrite?: boolean;
  includePhoto?: boolean;
  photoUrl?: string;
  includePhone?: boolean;
  phone?: string;
  includeLinkedIn?: boolean;
  linkedIn?: string;
  includeWebsite?: boolean;
  website?: string;
}

function buildParams(opts: ResumeOptions): Record<string, string | boolean> {
  const p: Record<string, string | boolean> = {template: opts.template};
  if (opts.aiRewrite) p.aiRewrite = true;
  if (opts.includePhoto) p.includePhoto = true;
  if (opts.photoUrl) p.photoUrl = opts.photoUrl;
  if (opts.includePhone && opts.phone) {
    p.includePhone = true;
    p.phone = opts.phone;
  }
  if (opts.includeLinkedIn && opts.linkedIn) {
    p.includeLinkedIn = true;
    p.linkedIn = opts.linkedIn;
  }
  if (opts.includeWebsite && opts.website) {
    p.includeWebsite = true;
    p.website = opts.website;
  }
  return p;
}

export const exportService = {
  /** Trigger AI cache warm-up in the background. Returns immediately. */
  async warmUpAi(portfolioId: number): Promise<void> {
    try {
      await apiClient.post(endpoints.portfolios.warmAi(portfolioId), null, {timeout: 30_000});
    } catch {}
  },

  /** Check if AI cache is warm (all content enhanced). */
  async isAiReady(portfolioId: number): Promise<boolean> {
    try {
      const resp = await apiClient.get<ApiResponse<{ready: boolean}>>(
        endpoints.portfolios.aiStatus(portfolioId),
        {timeout: 30_000},
      );
      return resp.data.data?.ready ?? false;
    } catch {
      return false;
    }
  },

  async generatePdf(
    portfolioId: number,
    opts: ResumeOptions,
  ): Promise<ExportResponse> {
    const resp = await apiClient.post<ApiResponse<ExportResponse>>(
      endpoints.portfolios.exportPdf(portfolioId),
      null,
      {params: buildParams(opts), timeout: 120_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async getPreviewHtml(
    portfolioId: number,
    opts: ResumeOptions,
  ): Promise<string> {
    const resp = await apiClient.get<string>(
      endpoints.portfolios.exportPreview(portfolioId),
      {params: buildParams(opts), responseType: 'text' as any, timeout: 120_000},
    );
    return resp.data as unknown as string;
  },

  async getPdfBase64(
    portfolioId: number,
    opts: ResumeOptions,
  ): Promise<string> {
    const resp = await apiClient.post<ApiResponse<{base64: string}>>(
      endpoints.portfolios.exportPdfInline(portfolioId),
      null,
      {params: buildParams(opts), timeout: 120_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!.base64;
  },

  // ─── Saved Resumes ──────────────────────────────────────────────

  /** Generate a PDF and save it permanently. */
  async generateAndSave(
    portfolioId: number,
    opts: ResumeOptions,
    title?: string,
  ): Promise<SavedResumeInfo> {
    const params: Record<string, string | boolean> = buildParams(opts);
    if (title) params.title = title;
    const resp = await apiClient.post<ApiResponse<SavedResumeInfo>>(
      endpoints.savedResumes.save(portfolioId),
      null,
      {params, timeout: 120_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  /** List all saved resumes for the current user. */
  async listSaved(): Promise<SavedResumeInfo[]> {
    const resp = await apiClient.get<ApiResponse<SavedResumeInfo[]>>(
      endpoints.savedResumes.list,
      {timeout: 60_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data ?? [];
  },

  /** Get saved PDF as base64.  */
  async getSavedBase64(id: number): Promise<string> {
    const resp = await apiClient.get<ApiResponse<{base64: string}>>(
      endpoints.savedResumes.base64(id),
      {timeout: 60_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!.base64;
  },

  /** Delete a saved resume. */
  async deleteSaved(id: number): Promise<void> {
    await apiClient.delete(endpoints.savedResumes.delete(id), {timeout: 30_000});
  },

  /** Publish a saved resume — returns updated info with publicUrl. */
  async publishResume(id: number): Promise<SavedResumeInfo> {
    const resp = await apiClient.post<ApiResponse<SavedResumeInfo>>(
      endpoints.savedResumes.publish(id),
      null,
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  /** Unpublish a saved resume — removes the public link. */
  async unpublishResume(id: number): Promise<SavedResumeInfo> {
    const resp = await apiClient.delete<ApiResponse<SavedResumeInfo>>(
      endpoints.savedResumes.publish(id),
      {timeout: 30_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },
};
