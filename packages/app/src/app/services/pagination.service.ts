import { Injectable } from '@angular/core';
import { GqlPagination } from '../../generated/graphql';

export type Pagination = Pick<
  GqlPagination,
  'totalPages' | 'page' | 'isLast' | 'isFirst' | 'isEmpty'
>;

@Injectable({
  providedIn: 'root',
})
export class PaginationService {
  constructor() {}
}
