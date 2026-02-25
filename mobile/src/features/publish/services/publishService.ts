import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {PublishResponse} from '../../../api/types/portfolio';
import type {ApiResponse} from '../../../api/types/common';

export const publishService = {
  async publish(portfolioId: number): Promise<PublishResponse> {
    const resp = await apiClient.post<ApiResponse<PublishResponse>>(
      endpoints.portfolios.publish(portfolioId),
    );
    if (resp.data.error) throw new Error(resp.data.error.message);
    return resp.data.data!;
  },

  async unpublish(portfolioId: number): Promise<void> {
    await apiClient.delete(endpoints.portfolios.publish(portfolioId));
  },

  async updateTheme(portfolioId: number, themeKey: string): Promise<void> {
    await apiClient.patch(endpoints.portfolios.detail(portfolioId), {themeKey});
  },
};
