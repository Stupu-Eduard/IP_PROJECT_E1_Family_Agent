export interface ExpenseDTO {
  amount: number;
  category: string;
  date: string;
  description?: string;
  person?: string;
  userId?: string | number | null;
  locationId?: number | null;
}