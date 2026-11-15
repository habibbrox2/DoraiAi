# Dorai AI: চার ব্যাকএন্ড ইন্টিগ্রেশন বাস্তবায়ন চেকলিস্ট

**শাখা (Branch):** `dorai_ai_free_integrations` (মূল `main` শাখা সম্পূর্ণ অক্ষত)
**নীতিমালা:** Single Responsibility per Platform, Zero Overhead, KMP Common-First Architecture, Local-First Privacy.

---

## ব্যাচ ও অগ্রগতি চেকলিস্ট (Execution Checklist)

### [x] ব্যাচ ১: Cloudflare Text Embeddings & AI Inference Enhancement (সম্পন্ন)
> **লক্ষ্য:** Cloudflare Workers AI-এর ফ্রি টেক্সট এম্বেডিং (`@cf/baai/bge-small-en-v1.5`) ইন্টিগ্রেট করা, যা পরবর্তী ব্যাচের Supabase pgvector মেমরির ভিত্তি হবে।
- [x] `CloudflareEmbeddingDto.kt`: রিকোয়েস্ট ও রেসপন্স DTO তৈরি (`commonMain/network/dtos/cloudflare/`).
- [x] `CloudflareEmbeddingClient.kt`: Ktor-ভিত্তিক এম্বেডিং কল ও ত্রুটি হ্যান্ডলিং.
- [x] ইউনিট টেস্ট: মক রেসপন্স পার্সিং ও অ্যাকাউন্ট আইডি ভ্যালিডেশন টেস্ট.
- [x] `spotlessCheck` ও টেস্ট ভেরিফিকেশন (সব টেস্ট গ্রিন ও সফল).

---

### [x] ব্যাচ ২: Supabase Client & pgvector Long-Term Memory (RAG) (সম্পন্ন)
> **লক্ষ্য:** কোনো বাহ্যিক ভারী SDK ছাড়াই Ktor দিয়ে হালকা PostgREST ক্লায়েন্ট তৈরি এবং ক্লাউড ভেক্টর মেমরি সংযোগ।
- [x] `SupabaseConfig.kt` ও `SupabaseRestClient.kt`: PostgREST REST ক্লায়েন্ট (Auth, Headers, RPC).
- [x] `SupabaseVectorStore.kt`: ভেক্টর এন্ট্রি ইনসার্ট ও `match_memories` কোসাইন সিমিলারিটি কোয়েরি.
- [x] `AppSettings.kt` কনফিগারেশন সাপোর্ট (`KEY_SUPABASE_URL`, `KEY_SUPABASE_ANON_KEY`, `KEY_SUPABASE_VECTOR_MEMORY_ENABLED`).
- [x] ইউনিট টেস্ট ও `spotlessCheck` গ্রিন (টেস্ট সফল).

---

### [x] ব্যাচ ৩: Supabase E2EE Cross-Device Sync (সম্পন্ন)
> **লক্ষ্য:** সেটিংস ও চ্যাট হিস্ট্রি ডিভাইসেই AES-256-GCM এনক্রিপ্ট করে Supabase-এ সুরক্ষিত সিঙ্ক করা।
- [x] `E2EECrypto.kt`: AES-256-GCM এনক্রিপশন ও পাসফ্রেজ কী-ডেরাইভেশন (`jvmShared`, `iosMain`, `wasmJsMain`).
- [x] `SupabaseSyncRecordDto.kt`: সিঙ্ক রেকর্ড পে-লোড DTO.
- [x] `CloudSyncService.kt`: জিরো-নলেজ ব্যাকআপ আপলোড, ডাউনলোড ও ডিক্রিপশন রিস্টোর লজিক.
- [x] `E2EESyncTest.kt`: এনক্রিপশন, ডিক্রিপশন ও চেকসাম ভ্যালিডেশন টেস্ট.
- [x] ফাইলসমূহ মূল ওয়ার্কস্পেসে (`G:\Web\DoraiAi-migration-worktree`) স্থানান্তরিত.

---

### [x] ব্যাচ ৪: Firebase Cloud Messaging (FCM) & Remote Config (সম্পন্ন)
> **লক্ষ্য:** ব্যাকগ্রাউন্ড টাস্ক কমপ্লিশন পুশ নোটিফিকেশন এবং ডাইনামিক মডেল ক্যাটালগ।
- [x] `FirebaseConfig.kt`: প্রজেক্ট আইডি ও এপিআই কি ভ্যালিডেশন.
- [x] `FirebaseRemoteConfigClient.kt`: KMP `commonMain`-এ REST-ভিত্তিক রিমোট কনফিগ ফেচার.
- [x] `FirebaseMessagingClient.kt`: FCM v1 REST ক্লায়েন্ট (টাস্ক কমপ্লিশন পুশ নোটিফিকেশন অ্যালার্ট).
- [x] `AppSettings.kt` কনফিগারেশন সাপোর্ট (`KEY_FIREBASE_PROJECT_ID`, `KEY_FIREBASE_API_KEY`, `KEY_FCM_DEVICE_TOKEN`, `KEY_FIREBASE_ALERTS_ENABLED`).
- [x] `FirebaseTest.kt`: কনফিগ, রিমোট কনফিগ পার্সিং ও বাংলা পুশ অ্যালার্ট পেলোড টেস্ট.
- [x] ফাইলসমূহ মূল ওয়ার্কস্পেসে (`G:\Web\DoraiAi-migration-worktree`) স্থানান্তরিত.

---

### [x] ব্যাচ ৫: GitHub Desktop Runner & Release Automation (সম্পন্ন)
> **লক্ষ্য:** ডেক্সটপ কোডিং এজেন্টের জন্য Git CLI সক্রিয়করণ ও OTA রিলিজ চ্যানেল নিশ্চিতকরণ।
- [x] `DesktopGitCommandRunner.kt` ফিক্স: `clone` সাবকমান্ডের জন্য `projectsRoot` cwd এবং ওভাররাইট গার্ড ফিক্স.
- [x] `GitCredentialBroker.desktop.kt` ফিক্স: উইন্ডোজ স্পেস-যুক্ত পাথের জন্য হেল্পার স্ক্রিপ্ট কোটেশন হ্যান্ডলিং (`!\"$path\"`).
- [x] `DesktopWorkspaceController.kt`: `WorkspaceController` ইন্টারফেস বাস্তবায়ন ও ডেক্সটপ গিট রানার সংযোগ.
- [x] `WorkspaceController.jvm.kt` ওয়্যারিং: `NoOpWorkspaceController`-এর বদলে `DesktopWorkspaceController` রিটার্ন.
- [x] `DesktopWorkspaceControllerTest.kt`: ক্লোন, ফেচ, স্ট্যাটাস ও লগ পার্সিং টেস্ট.
- [x] ফাইলসমূহ মূল ওয়ার্কস্পেসে (`G:\Web\DoraiAi-migration-worktree`) স্থানান্তরিত.

---

## পরবর্তী পর্যায়: UI সংযোগ ও লাইভ Hybrid RAG মেমরি ইনজেকশন (Next Phases)

### [x] পরবর্তী ফেজ ১: UI সেটিংস ও ইউজার কন্ট্রোল (সম্পন্ন)
- [x] Compose Resources-এ নতুন স্ট্রিং ও লেবেল যোগ (`commonMain/composeResources/` - বাংলা ও ইংরেজি).
- [x] `CloudVaultSection.kt`: Supabase E2EE Cloud Vault কার্ড (পাসফ্রেজ ইনপুট, ব্যাকআপ ও রিস্টোর অ্যাকশন বাটন, জিরো-নলেজ নোটিস).
- [x] `ExportImportSection.kt`-এ CloudVaultCard ইন্টিগ্রেশন.
- [x] `FirebaseAlertsSection.kt`: Firebase Remote Alerts & FCM পুশ নোটিফিকেশন কার্ড ও টেস্ট অ্যালার্ট বাটন.
- [x] `AgentSettings.kt`-এ FirebaseAlertsCard ইন্টিগ্রেশন.
- [x] `SettingsUiState.kt` ও `SettingsActions.kt` প্রপার্টি ম্যাপিং.
- [x] ফাইলসমূহ মূল ওয়ার্কস্পেসে (`G:\Web\DoraiAi-migration-worktree`) স্থানান্তরিত.

### [ ] পরবর্তী ফেজ ২: চ্যাটে Hybrid RAG মেমরি ইনজেকশন (Live Context Retrieval)
- [ ] `ChatSystemPromptBuilder.kt`: `retrievedContext` (সেমিকলনিক্যালি প্রাসঙ্গিক ভেক্টর মেমরি) প্রম্পট সেকশন সাপোর্ট.
- [ ] `RemoteDataRepository.kt`: ইউজার মেসেজ পাঠানোর সময় ক্লাউডফ্লেয়ার এম্বেডিং + সুপাবেস ভেক্টর ম্যাচ (১.৫ সেকেন্ড বাউন্ডেড টাইমআউট ও স্মুথ অফলাইন ফলব্যাক).
- [ ] অ্যাসিনক্রোনাস রাইট-থ্রু মেমরি লার্নিং (লোকাল মেমরি সেভের পর ব্যাকগ্রাউন্ডে ক্লাউডে ভেক্টর আপসার্ট).

### [ ] পরবর্তী ফেজ ৩: ডকুমেন্টেশন ও রিলিজ প্রস্তুতি (Docs & Changelog)
- [ ] `docs/features/github-integration.md` ডেক্সটপ সাপোর্ট আপডেট.
- [ ] ৩টি নতুন ফিচার স্পেক তৈরি (`docs/features/`): `supabase-e2ee-cloud-sync.md`, `hybrid-rag-vector-memory.md`, `firebase-alerts-remote-ops.md`.
- [ ] `CHANGELOG.md` আপডেট ও পূর্ণাঙ্গ `spotlessCheck` ভেরিফিকেশন.
