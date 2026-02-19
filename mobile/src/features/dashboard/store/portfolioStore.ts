import {create} from 'zustand';
import type {Portfolio} from '../../../api/types/portfolio';
import {portfolioService} from '../services/portfolioService';

type PortfolioState = {
  portfolios: Portfolio[];
  loading: boolean;
  error: string | null;
  fetchPortfolios: () => Promise<void>;
  addPortfolio: (p: Portfolio) => void;
  setPortfolios: (ps: Portfolio[]) => void;
  removePortfolio: (id: number) => void;
};

export const usePortfolioStore = create<PortfolioState>(set => ({
  portfolios: [],
  loading: false,
  error: null,

  fetchPortfolios: async () => {
    set({loading: true, error: null});
    try {
      const portfolios = await portfolioService.list();
      set({portfolios, loading: false});
    } catch (e: unknown) {
      set({
        error: e instanceof Error ? e.message : 'Failed to load portfolios',
        loading: false,
      });
    }
  },

  addPortfolio: (p: Portfolio) =>
    set(state => ({
      portfolios: [p, ...state.portfolios.filter(pp => pp.id !== p.id)],
    })),

  setPortfolios: (ps: Portfolio[]) => set({portfolios: ps}),

  removePortfolio: (id: number) =>
    set(state => ({portfolios: state.portfolios.filter(p => p.id !== id)})),
}));
