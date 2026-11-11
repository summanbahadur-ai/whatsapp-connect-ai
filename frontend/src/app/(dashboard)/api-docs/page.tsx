import type { Metadata } from 'next';
import { ApiDocsClient } from '@/components/api-docs/api-docs-client';
import 'swagger-ui-dist/swagger-ui.css';

export const metadata: Metadata = {
  title: 'API documentation | Omni Connect',
  robots: { index: false, follow: false },
};
export default function ApiDocsPage() {
  return <ApiDocsClient />;
}
