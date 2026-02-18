export type ApiResponse<T> = {
  data: T;
  error: null;
  meta: ApiMeta | null;
} | {
  data: null;
  error: ApiError;
  meta: null;
};

export type ApiError = {
  code: string;
  message: string;
};

export type ApiMeta = {
  total: number;
  page: number;
  size: number;
};
