export interface ExpenseDTO {
  amount: number;
  category: string;
  date: string;
  currency?: string;
  description?: string;
  locationName?: string;
}