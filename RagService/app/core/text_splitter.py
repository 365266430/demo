import re


class TextSplitter:
    def __init__(self, chunk_size: int = 900, chunk_overlap: int = 150):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

    def split(self, text: str) -> list[str]:
        normalized = re.sub(r"\s+", " ", text or "").strip()
        if not normalized:
            return []

        chunks: list[str] = []
        start = 0
        length = len(normalized)

        while start < length:
            end = min(start + self.chunk_size, length)
            chunk = normalized[start:end].strip()
            if chunk:
                chunks.append(chunk)
            if end == length:
                break
            start = max(end - self.chunk_overlap, start + 1)

        return chunks


text_splitter = TextSplitter()

