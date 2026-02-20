import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';

export const previewService = {
  async getHtml(portfolioId: number): Promise<string> {
    const resp = await apiClient.get<string>(endpoints.portfolios.preview(portfolioId), {
      headers: {Accept: 'text/html'},
      // Return raw string, not JSON
      transformResponse: [(data: string) => data],
    });
    return resp.data;
  },
};
