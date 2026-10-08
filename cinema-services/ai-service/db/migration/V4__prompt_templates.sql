-- Migration V4: Dynamic Prompt Templates for LLM Subsystems

CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    prompt_code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    system_prompt TEXT NOT NULL,
    user_template TEXT NOT NULL,
    model_name VARCHAR(64) NOT NULL DEFAULT 'gpt-4o-mini',
    temperature NUMERIC(3, 2) NOT NULL DEFAULT 0.20,
    max_tokens INT NOT NULL DEFAULT 200,
    required_variables JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_code ON prompt_templates(prompt_code);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_active ON prompt_templates(is_active);

-- Seed initial default prompt templates for the 4 core AI subsystems

INSERT INTO prompt_templates (
    prompt_code, name, description, system_prompt, user_template,
    model_name, temperature, max_tokens, required_variables, is_active, version
) VALUES
(
    'QUERY_REWRITE',
    'Chatbot Query Rewriter',
    'Resolves conversational ellipsis and rewrites follow-up messages into standalone search queries',
    'You are an expert at resolving conversational ellipsis and rewriting follow-up queries into clear standalone search queries.',
    'Given the conversational dialogue between a User and a Cinema AI Assistant:
{formatted_history}

Latest user message: "{current_message}"
Most recently discussed movie: "{last_movie_title}"

Task: Rewrite the user''s latest message into a fully resolved, standalone search query preserving all implicit context and entity mentions from previous turns. Return ONLY the rewritten query text without explanation or greeting.',
    'gpt-4o-mini',
    0.00,
    100,
    '["formatted_history", "current_message", "last_movie_title"]'::jsonb,
    TRUE,
    1
),
(
    'CHATBOT_GROUNDED_REPLY',
    'PopBot Grounded Generator',
    'Generates polite cinema assistant replies strictly grounded in retrieved catalog entities without hallucination',
    'You are PopBot, AI assistant for CinePremier cinema. Ground all responses strictly in the provided movies.',
    'Catalog data retrieved from CinePremier: {titles_str}.
User request: "{user_message}".
Write 1-2 natural, polite sentences introducing these movies. ONLY mention movies present in the retrieved list above. Do NOT hallucinate unlisted movies.',
    'gpt-4o-mini',
    0.20,
    150,
    '["titles_str", "user_message"]'::jsonb,
    TRUE,
    1
),
(
    'SENTIMENT_ASPECT_ANALYSIS',
    'Review Aspect Sentiment & Consistency Analyzer',
    'Analyzes review sentiment across plot, acting, visuals, audio and flags sarcastic or contradictory ratings',
    'You are an expert NLP cinema sentiment and aspect analyzer. Output strictly valid JSON.',
    'User review for a cinema movie:
- Star rating given by user: {rating} / 5.0
- Review comment: "{clean_text}"

Analyze the review and return a valid JSON object with the following fields:
- "sentiment_score": float between -1.0 (most negative) and 1.0 (most positive)
- "sentiment_label": string, either "POSITIVE", "NEGATIVE", or "NEUTRAL"
- "feedback_consistency": string, "CONSISTENT" if rating matches comment tone, or "INCONSISTENT" if contradictory (e.g. 5 stars but harsh complaints, 1 star but glowing praise, sarcasm, spam, or off-topic)
- "confidence_score": float between 0.0 and 1.0 (lower <= 0.35 if sarcastic, contradictory, or noisy; high >= 0.85 if clear and consistent)
- "aspect_sentiment": object with keys "plot", "acting", "visuals", "audio" containing score between -1.0 and 1.0 for aspects mentioned in comment

Return ONLY the raw JSON object without markdown formatting, code block, or explanation.',
    'gpt-4o-mini',
    0.00,
    250,
    '["rating", "clean_text"]'::jsonb,
    TRUE,
    1
),
(
    'RECOMMENDATION_EXPLAINER',
    'Personalized Recommendation Rationale Explainer',
    'Generates a single concise sentence explaining why a specific movie was recommended based on genres and history',
    'Bạn là chuyên viên đề xuất phim của CinePremier. Giải thích chính xác, súc tích và có căn cứ thực tế.',
    'Phim được đề xuất: ''{movie_title}'' (Thể loại: {genres_str}).
Lịch sử xem gần đây của người dùng: Thích các phim thể loại {recent_str}.
Nguồn gợi ý thuật toán: {source}.

Nhiệm vụ: Viết đúng 1 câu tiếng Việt ngắn gọn (dưới 25 từ), tự nhiên và lịch sự để giải thích tại sao hệ thống rạp CinePremier đề xuất phim này cho người dùng.
YÊU CẦU BẮT BUỘC: CHỈ ĐƯỢC dựa vào các thông tin thể loại và lịch sử cung cấp ở trên. Tuyệt đối không bịa đặt nội dung phim hoặc diễn viên không có trong dữ liệu.',
    'gpt-4o-mini',
    0.15,
    80,
    '["movie_title", "genres_str", "recent_str", "source"]'::jsonb,
    TRUE,
    1
)
ON CONFLICT (prompt_code) DO NOTHING;
