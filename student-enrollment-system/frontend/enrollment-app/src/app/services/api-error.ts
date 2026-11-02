import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(response: HttpErrorResponse): string {
  const body = response.error;
  if (typeof body === 'string' && body) return body;
  if (body?.error) return body.error;
  if (body && typeof body === 'object') {
    const fields = Object.entries(body);
    if (fields.length) return fields.map(([field, message]) => `${field}: ${message}`).join(' | ');
  }
  return `Request failed (HTTP ${response.status}).`;
}
