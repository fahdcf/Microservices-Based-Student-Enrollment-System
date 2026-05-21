export interface Student {
  id: number;
  cnie: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface StudentRequest {
  cnie: string;
  firstName: string;
  lastName: string;
  email: string;
}
