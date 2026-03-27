export async function downloadFile(url: string, token?: string, fallbackFilename = 'report.csv') {
  const response = await fetch(url, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      Accept: 'text/csv,*/*'
    }
  });

  if (!response.ok) {
    throw new Error('下载失败');
  }

  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') || '';
  const filename = resolveFilename(disposition, fallbackFilename);
  const objectUrl = URL.createObjectURL(
    blob.type ? blob : new Blob([blob], { type: 'text/csv;charset=utf-8' })
  );
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

function resolveFilename(contentDisposition: string, fallbackFilename: string) {
  const filenameStarMatch = contentDisposition.match(/filename\*=([^;]+)/i);
  if (filenameStarMatch?.[1]) {
    const candidate = decodeRfc5987(filenameStarMatch[1]);
    if (candidate) {
      return ensureCsvExtension(candidate);
    }
  }

  const filenameMatch = contentDisposition.match(/filename="([^"]+)"|filename=([^;]+)/i);
  const rawFilename = filenameMatch?.[1] || filenameMatch?.[2];
  if (rawFilename) {
    const candidate = decodePlainFilename(rawFilename.trim());
    if (candidate) {
      return ensureCsvExtension(candidate);
    }
  }

  return ensureCsvExtension(fallbackFilename);
}

function decodeRfc5987(value: string) {
  const normalized = value.trim().replace(/^UTF-8''/i, '').replace(/^"(.*)"$/, '$1');
  try {
    return decodeURIComponent(normalized);
  } catch {
    return normalized;
  }
}

function decodePlainFilename(value: string) {
  const cleaned = value.replace(/^"(.*)"$/, '$1');
  const rfc2047Match = cleaned.match(/^=\?UTF-8\?Q\?(.+)\?=$/i);
  if (!rfc2047Match?.[1]) {
    return cleaned;
  }

  const decoded = rfc2047Match[1]
    .replace(/_/g, ' ')
    .replace(/=([0-9A-F]{2})/gi, (_, hex: string) => String.fromCharCode(parseInt(hex, 16)));

  try {
    return decodeURIComponent(
      Array.from(decoded)
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    );
  } catch {
    return decoded;
  }
}

function ensureCsvExtension(filename: string) {
  return /\.csv$/i.test(filename) ? filename : `${filename}.csv`;
}
