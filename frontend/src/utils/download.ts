export async function downloadFile(url: string, token?: string) {
  const response = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined
  });
  if (!response.ok) {
    throw new Error('下载失败');
  }
  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename\*?=(?:UTF-8''|\")?([^;\"]+)/i);
  const filename = decodeURIComponent((match?.[1] || 'report.csv').replace(/\"/g, ''));
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(objectUrl);
}