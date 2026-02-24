import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {
  ResumeInfo,
  CreateResumeRequest,
  UpdateResumeRequest,
  TemplateInfo,
} from '../../../api/types/resume';
import type {ExportResponse} from '../../../api/types/portfolio';
import type {ApiResponse} from '../../../api/types/common';

export const resumeService = {
  async list(): Promise<ResumeInfo[]> {
    const resp = await apiClient.get<ApiResponse<ResumeInfo[]>>(
      endpoints.resumes.list,
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async get(resumeId: number): Promise<ResumeInfo> {
    const resp = await apiClient.get<ApiResponse<ResumeInfo>>(
      endpoints.resumes.detail(resumeId),
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async create(req: CreateResumeRequest): Promise<ResumeInfo> {
    const resp = await apiClient.post<ApiResponse<ResumeInfo>>(
      endpoints.resumes.list,
      req,
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async update(
    resumeId: number,
    req: UpdateResumeRequest,
  ): Promise<ResumeInfo> {
    const resp = await apiClient.patch<ApiResponse<ResumeInfo>>(
      endpoints.resumes.detail(resumeId),
      req,
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async delete(resumeId: number): Promise<void> {
    await apiClient.delete(endpoints.resumes.detail(resumeId));
  },

  async getPreviewHtml(resumeId: number): Promise<string> {
    const resp = await apiClient.get<string>(
      endpoints.resumes.preview(resumeId),
      {
        headers: {Accept: 'text/html'},
        transformResponse: [(data: string) => data],
      },
    );
    return resp.data;
  },

  async getTemplatePreviewHtml(
    resumeId: number,
    templateKey: string,
  ): Promise<string> {
    const resp = await apiClient.get<string>(
      endpoints.resumes.previewWithTemplate(resumeId, templateKey),
      {
        headers: {Accept: 'text/html'},
        transformResponse: [(data: string) => data],
      },
    );
    return resp.data;
  },

  async generatePdf(resumeId: number): Promise<ExportResponse> {
    const resp = await apiClient.post<ApiResponse<ExportResponse>>(
      endpoints.resumes.pdf(resumeId),
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async getTemplates(): Promise<TemplateInfo[]> {
    const resp = await apiClient.get<ApiResponse<TemplateInfo[]>>(
      endpoints.resumes.templates,
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async fetchPdfBase64(resumeId: number): Promise<string> {
    const resp = await apiClient.get<ApiResponse<{base64: string}>>(
      endpoints.resumes.pdfInline(resumeId),
      {timeout: 120000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!.base64;
  },
};
