# GroupMind

**Turn a WhatsApp group chat into a searchable, cited, consensus-aware knowledge base.**

Ask a question in plain language — *"who do people recommend for AC repair?"* — and GroupMind
answers from the group's own messages, cites exactly which ones, and tells you **how many
distinct people independently agreed**. That corroboration count is the whole point: it turns
search into something that feels like asking a well-informed neighbour.

Built with Java 21 + Spring Boot + Spring AI. Runs out of the box with **no API keys and no
database** (in-memory vector store + local embeddings), and scales up to pgvector + Claude
synthesis by flipping two switches.

---

## Why RAG here (and not plain keyword search or a bare LLM)

WhatsApp's own search fails because of *phrasing variance* — "AC repair guy", "cooling mechanic",
"aircon technician" and "cool care wala" share almost no keywords but mean the same thing. A bare
LLM fails too: it has no knowledge of *your* group. Semantic retrieval matches on meaning, so all
those phrasings come back together — which is exactly what lets the consensus step count that
**8 different residents** pointed at the same vendor.

---

## Architecture

```
greenwood_residency_chat.txt
        │  WhatsAppParser        (timestamp, sender, text; skips system/multi-line quirks)
        ▼
   IngestionService             one message = one chunk, embedded on startup
        │  local ONNX all-MiniLM-L6-v2  →  VectorStore (in-memory | pgvector)
        ▼
POST /api/query  ──►  RetrievalService     top-k semantic search
                            │
                            ▼
                     ConsensusService       cluster near-dup mentions,
                            │                count DISTINCT senders
                            ▼
                     SynthesisService        answer + citations + backer count
                            │                (Claude if configured, else grounded template)
                            ▼
                     static/index.html       query box + consensus badge + citations
```

---

## Prerequisites

- **JDK 21** (`java -version` → 21) and **Maven 3.9+** (`mvn -v`).
- Internet access on first run: the embedding model (`all-MiniLM-L6-v2`, ~90 MB) downloads once
  and is cached under `~/.spring-ai`.
- *(Optional)* Docker, only if you want to run the pgvector path.
- *(Optional)* An `ANTHROPIC_API_KEY`, only if you want Claude to write the final prose.

---

## Quick start (zero config — recommended for the demo)

In-memory vector store, local embeddings, deterministic grounded answers. No keys, no DB.

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080** and ask:

> who do people recommend for AC repair?

You'll get a **"8 residents agree"** badge, the vendor surfaced from the messages, and the
citations it's grounded in. Try the electrician (4 backers) and plumber (3 backers) chips too.

The sample dataset (`src/main/resources/data/greenwood_residency_chat.txt`, 199 synthetic
messages across 17 residents) is loaded automatically on startup — no manual import step.

Sanity check the backend directly:

```bash
curl -s localhost:8080/api/health
curl -s -X POST localhost:8080/api/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"who do people recommend for AC repair?"}' | jq
```

---

## Optional: turn on Claude for the final answer

By default the answer is a grounded, deterministic summary so the app needs no keys. To have
Claude synthesise the prose instead (still grounded strictly in the retrieved messages):

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export GROUPMIND_CHAT_MODEL=anthropic
mvn spring-boot:run
```

The response's `llmUsed` flag (and the pill in the UI) shows which path produced the answer.
Retrieval and consensus are identical either way — only the wording of the final answer changes.

---

## Optional: use pgvector instead of the in-memory store

The default in-memory `SimpleVectorStore` needs nothing external. To use Postgres + pgvector:

**1. Start Postgres with the pgvector extension:**

```bash
docker run --name groupmind-pg -e POSTGRES_DB=groupmind \
  -e POSTGRES_USER=groupmind -e POSTGRES_PASSWORD=groupmind \
  -p 5432:5432 -d pgvector/pgvector:pg16
```

**2. Run with the pgvector Maven profile + Spring profile active:**

```bash
SPRING_PROFILES_ACTIVE=pgvector mvn spring-boot:run -Ppgvector
```

The `-Ppgvector` Maven profile pulls in `spring-ai-starter-vector-store-pgvector` (kept out of the
default build so the DB-free path has zero Postgres dependencies). The `pgvector` Spring profile
switches off the in-memory bean and lets Spring AI auto-configure a `PgVectorStore` from
`application-pgvector.yml` (schema + extension created automatically on first run, cosine/HNSW,
384 dims to match the embedding model). Everything else in the app depends only on the
`VectorStore` interface, so nothing else changes.

DB connection defaults can be overridden with `GROUPMIND_DB_URL`, `GROUPMIND_DB_USER`,
`GROUPMIND_DB_PASSWORD`.

---

## Configuration reference

| Setting | Env / property | Default | Purpose |
|---|---|---|---|
| Chat model | `GROUPMIND_CHAT_MODEL` / `spring.ai.model.chat` | `none` | `anthropic` enables Claude synthesis |
| Anthropic key | `ANTHROPIC_API_KEY` | *(empty)* | required only when chat model is `anthropic` |
| Retrieval size | `groupmind.top-k` | `10` | messages pulled per query |
| Consensus threshold | `groupmind.consensus-min-senders` | `2` | distinct backers needed to count as consensus |
| Dataset | `groupmind.data-path` | bundled `.txt` | swap in another WhatsApp export |
| Vector store | Spring profile `pgvector` | in-memory | see pgvector section |

To point at your own export, drop the `.txt` in `src/main/resources/data/` and set
`groupmind.data-path`. Use only synthetic or consented data — see the note below.

---

## How the two hard parts work

**Parser** (`WhatsAppParser`). Matches the native export header
`DD/MM/YY, HH:MM - Sender: message`. Lines with no `Sender:` (the E2E-encryption notice,
"created group", "added X", "joined via invite link") are treated as system lines and skipped;
lines with no header are appended to the message in progress, so multi-line messages stay intact.
Verified against the sample: 199 messages, 17 senders, zero system lines leaking through.

**Consensus** (`ConsensusService`). The vendor's name is written inconsistently and usually
lowercase, so this is deliberately a lightweight *lexical* clustering, not a second embedding pass:
tokenize case-insensitively, drop English/chat glue **and household-service category words**
(`repair`, `cooling`, `electrician`…) so the *name* surfaces rather than the category everyone
shares; count distinct senders per token; union-find-merge tokens that co-occur in the same
messages ("ramesh" + "cool" + "care" → one entity); rank by distinct-sender count. On short,
name-bearing chat messages this is fast, explainable, and attributes each mention to exactly one
sender — so the count you see is trustworthy. Verified counts on the sample: AC → 8, electrician
→ 4, plumber → 3.

---

## Project layout

```
src/main/java/com/betterplace/groupmind/
  GroupMindApplication.java        boot entrypoint
  config/GroupMindProperties.java  groupmind.* tunables
  config/VectorStoreConfig.java    in-memory store (off under pgvector profile)
  model/ChatMessage.java           parsed message record
  parse/WhatsAppParser.java        export → structured messages
  ingest/IngestionService.java     parse + embed + store on startup
  retrieve/RetrievalService.java   top-k semantic search
  retrieve/RetrievedMessage.java
  consensus/ConsensusService.java  distinct-backer clustering  ← the clever bit
  consensus/ConsensusCluster.java
  synthesis/SynthesisService.java  Claude synthesis OR grounded template
  api/QueryController.java         POST /api/query, GET /api/health, POST /api/ingest
  api/dto/QueryRequest.java, QueryResponse.java
src/main/resources/
  application.yml                  in-memory + key-free defaults
  application-pgvector.yml         pgvector profile
  data/greenwood_residency_chat.txt  199-message synthetic sample
  static/index.html                demo UI
```

---

## API

`POST /api/query`
```json
{ "question": "who do people recommend for AC repair?", "topK": 10 }
```
returns
```json
{
  "question": "...",
  "answer": "8 residents independently point to \"ramesh\" ...",
  "consensus": [
    { "label": "ramesh", "distinctSenderCount": 8, "senders": ["Meena Krishnan", "..."], "messageIds": [12, 47, "..."] }
  ],
  "citations": [ { "id": 12, "sender": "Meena Krishnan", "timestamp": "2025-05-11T09:42", "text": "...", "score": 0.71 } ],
  "llmUsed": false
}
```
`GET /api/health` → ingest status. `POST /api/ingest` → manual re-ingest.

---

## A note on data

The bundled dataset is **entirely synthetic** — invented residents, invented vendors, invented
events. Don't point GroupMind at a real group's export without the members' consent; building a
demo on people's private messages isn't worth it no matter how good the intent.

---

## Notes / caveats

- The deterministic parser and consensus algorithm were verified directly against the sample
  dataset (see `verify/`). The Spring wiring targets **Spring AI 1.0.0**; if you're on a
  different Spring AI version, a couple of builder/method names may need a nudge, but the pipeline
  shape is unchanged.
- First run is slower while the embedding model downloads and messages are embedded; subsequent
  runs are fast (model is cached).
