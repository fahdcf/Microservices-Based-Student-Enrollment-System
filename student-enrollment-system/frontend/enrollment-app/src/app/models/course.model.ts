export interface Course {
  id: number;
  title: string;
  description: string;
  credits: number;
}

export interface CourseRequest {
  title: string;
  description: string;
  credits: number;
}
