# Spring Boot ChatGPT Integration Project

Spring Boot와 Java/Groovy를 사용한 ChatGPT API 연동 프로젝트입니다.

## 기술 스택

- **Spring Boot 3.2.0**
- **Java 17**
- **Groovy 4.0**
- **OpenAI API (GPT-4)**

## 주요 기능

### 1. 기본 채팅 기능
- 단순 프롬프트 실행
- 시스템 메시지와 함께 프롬프트 실행
- 대화 히스토리 유지

### 2. 프롬프트 템플릿 (Groovy)
- 데이터 분석 프롬프트
- 텍스트 요약 프롬프트
- JSON 데이터 추출 프롬프트
- Q&A 프롬프트
- 코드 생성 프롬프트
- 번역 프롬프트
- 감정 분석 프롬프트

### 3. 데이터 처리 (Groovy)
- CSV 데이터 분석
- 구조화된 데이터 추출
- 배치 텍스트 처리
- 데이터 분류
- 데이터 검증 및 교정
- 컨텍스트 기반 질의응답

## 설정 방법

### 1. OpenAI API 키 설정

`application.properties` 파일을 수정하거나 환경 변수를 설정하세요:

```properties
openai.api.key=your-openai-api-key-here
```

또는 환경 변수로:

```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

### 2. 프로젝트 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

## API 엔드포인트

### 기본 채팅
```bash
POST /api/chatgpt/chat
{
  "prompt": "안녕하세요!"
}
```

### 텍스트 요약
```bash
POST /api/chatgpt/summarize
{
  "text": "요약할 긴 텍스트...",
  "maxLength": 200
}
```

### 데이터 추출
```bash
POST /api/chatgpt/extract
{
  "text": "홍길동 고객님의 주문번호는 ORD-001입니다.",
  "fields": ["name", "orderNumber"]
}
```

### CSV 데이터 분석
```bash
POST /api/chatgpt/analyze/csv
{
  "data": "이름,나이,직업\n홍길동,30,개발자\n김철수,25,디자이너"
}
```

### Q&A
```bash
POST /api/chatgpt/qa
{
  "context": "Spring Boot는 Java 기반의 웹 애플리케이션 프레임워크입니다.",
  "question": "Spring Boot는 무엇인가요?"
}
```

### 대화 (히스토리 유지)
```bash
POST /api/chatgpt/conversation
{
  "history": [
    {"role": "user", "content": "안녕하세요"},
    {"role": "assistant", "content": "안녕하세요! 무엇을 도와드릴까요?"}
  ],
  "message": "Spring Boot에 대해 알려주세요"
}
```

### 데이터 분류
```bash
POST /api/chatgpt/classify
{
  "text": "이 제품은 정말 훌륭합니다!",
  "categories": ["긍정", "부정", "중립"]
}
```

### 코드 생성
```bash
POST /api/chatgpt/code/generate
{
  "language": "Python",
  "description": "리스트를 정렬하는 함수",
  "requirements": ["bubble sort 사용", "주석 포함"]
}
```

### 번역
```bash
POST /api/chatgpt/translate
{
  "text": "안녕하세요",
  "sourceLang": "한국어",
  "targetLang": "영어"
}
```

### 감정 분석
```bash
POST /api/chatgpt/sentiment
{
  "text": "오늘 정말 좋은 하루였어요!"
}
```

### 데이터 검증
```bash
POST /api/chatgpt/validate
{
  "data": "test@email",
  "rules": ["이메일 형식 확인", "도메인 포함 여부"]
}
```

### 배치 처리
```bash
POST /api/chatgpt/batch
{
  "texts": ["텍스트1", "텍스트2", "텍스트3"],
  "type": "summarize"
}
```

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/example/chatgpt/
│   │   ├── ChatGptApplication.java          # 메인 애플리케이션
│   │   ├── config/
│   │   │   └── OpenAiConfig.java            # OpenAI 설정
│   │   ├── controller/
│   │   │   └── ChatGptController.java       # REST API 컨트롤러
│   │   ├── dto/
│   │   │   └── OpenAiDto.java               # OpenAI DTO
│   │   ├── service/
│   │   │   └── OpenAiService.java           # OpenAI 서비스
│   │   └── runner/
│   │       └── TestRunner.java              # 테스트 러너
│   ├── groovy/com/example/chatgpt/service/
│   │   ├── PromptTemplateService.groovy     # 프롬프트 템플릿 (Groovy)
│   │   └── DataProcessingService.groovy     # 데이터 처리 (Groovy)
│   └── resources/
│       └── application.properties            # 설정 파일
```

## 사용 예제

### Java에서 사용
```java
@Autowired
private OpenAiService openAiService;

public void example() {
    String response = openAiService.chat("안녕하세요!");
    System.out.println(response);
}
```

### Groovy에서 사용
```groovy
@Autowired
DataProcessingService dataProcessingService

def example() {
    def result = dataProcessingService.analyzeCSVData(csvData)
    println result.analysis
}
```

## 테스트 실행

`TestRunner.java`의 주석을 해제하여 테스트를 실행할 수 있습니다:

```java
// testSimpleChat();        // 주석 해제
// testDataExtraction();    // 주석 해제
// testSentimentAnalysis(); // 주석 해제
```

## 주의사항

1. OpenAI API 키가 필요합니다
2. API 사용량에 따라 비용이 발생할 수 있습니다
3. 프로덕션 환경에서는 적절한 에러 핸들링과 재시도 로직을 추가하세요
4. Rate limiting을 고려하여 배치 처리 시 적절한 딜레이를 추가하세요

## 확장 가능성

- 프롬프트 템플릿을 데이터베이스나 파일로 관리
- 캐싱을 통한 비용 절감
- 스트리밍 응답 지원
- 함수 호출(Function Calling) 기능 추가
- 임베딩(Embeddings)을 이용한 의미 검색
- 벡터 DB 연동

## 라이센스

MIT License
