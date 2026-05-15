from openai import OpenAI

from app.config import settings
from app.schemas import SourceChunk


class Generator:
    def answer(self, question: str, sources: list[SourceChunk]) -> str:
        if not sources:
            return "没有检索到与问题相关的文档片段。"

        if not settings.llm_api_key:
            return self._fallback_answer(sources)

        context = "\n\n".join(
            f"[片段 {source.chunk_index}]\n{source.content}" for source in sources
        )
        prompt = (
            "你是法律文档问答助手。请只基于给定资料回答问题；"
            "如果资料不足，请明确说明无法从文档中确认。\n\n"
            f"资料：\n{context}\n\n"
            f"问题：{question}"
        )

        client = OpenAI(api_key=settings.llm_api_key, base_url=settings.llm_base_url)
        response = client.chat.completions.create(
            model=settings.llm_model,
            temperature=settings.llm_temperature,
            messages=[
                {"role": "system", "content": "你擅长基于检索片段进行严谨的中文法律文档问答。"},
                {"role": "user", "content": prompt},
            ],
        )
        return response.choices[0].message.content or ""

    def _fallback_answer(self, sources: list[SourceChunk]) -> str:
        joined = "\n\n".join(
            f"片段 {source.chunk_index}: {source.content[:500]}" for source in sources[:3]
        )
        return "当前未配置 LLM_API_KEY，已返回最相关的检索片段：\n\n" + joined


generator = Generator()

