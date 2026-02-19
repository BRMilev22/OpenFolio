import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {
  Portfolio,
  CreatePortfolioRequest,
  IngestionRequest,
} from '../../../api/types/portfolio';
import type {ApiResponse} from '../../../api/types/common';

export const portfolioService = {
  async list(): Promise<Portfolio[]> {
    const resp = await apiClient.get<ApiResponse<Portfolio[]>>(endpoints.portfolios.list);
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async create(req: CreatePortfolioRequest): Promise<Portfolio> {
    const resp = await apiClient.post<ApiResponse<Portfolio>>(endpoints.portfolios.list, req);
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async delete(id: number): Promise<void> {
    await apiClient.delete(endpoints.portfolios.detail(id));
  },

  async ingestFromGitHub(req: IngestionRequest): Promise<Portfolio> {
    // Ingestion fetches GitHub data + kicks off AI — give it extra time
    const resp = await apiClient.post<ApiResponse<Portfolio>>(
      endpoints.ingestion.github,
      req,
      {timeout: 120_000},
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },
};
