import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {Project, Skill} from '../../../api/types/editor';
import type {ApiResponse} from '../../../api/types/common';

export const editorService = {
  async getProjects(portfolioId: number): Promise<Project[]> {
    const resp = await apiClient.get<ApiResponse<Project[]>>(
      endpoints.portfolios.projects(portfolioId),
    );
    if (resp.data.error) {throw new Error(resp.data.error.message);}
    return resp.data.data!;
  },

  async getSkills(portfolioId: number): Promise<Skill[]> {
    const resp = await apiClient.get<ApiResponse<Skill[]>>(
      endpoints.portfolios.skills(portfolioId),
    );
    if (resp.data.error) {throw new Error(resp.data.error.message);}
    return resp.data.data!;
  },
};
