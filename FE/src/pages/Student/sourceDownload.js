export function getSourceDownloadUrl(processingError) {
  return processingError?.match(/https?:\/\/\S+/)?.[0]?.replace(/\.$/, '') ?? null;
}
