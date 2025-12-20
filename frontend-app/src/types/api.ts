export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ApiMeta {
  pagination?: PaginationMeta;
}

export interface ApiResponse<T> {
  data: T;
  error?: ApiError;
  links?: Record<string, string>;
  meta?: ApiMeta;
  success?: boolean; // Optional field that may be added by gateway/middleware
}
