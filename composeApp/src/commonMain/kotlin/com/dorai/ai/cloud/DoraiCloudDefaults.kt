package com.dorai.ai.cloud

/** First-party Supabase project backing Dorai Cloud. */
const val DORAI_CLOUD_SUPABASE_URL = "https://rbzlvvrmihmvbbiomoqe.supabase.co"

/**
 * Supabase publishable key for the first-party project.
 *
 * Publishable keys are public client credentials (they are replaced by the
 * end-user's own access token on every call and RLS is the isolation
 * boundary), so shipping them in the app is expected — but they are never a
 * substitute for user authentication.
 */
const val DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY = "sb_publishable_DIjAop6LGA_TaPZbDzNkLA_9BUgZfga"

/** Firebase Web API key (public client identifier, restricted by Firebase project rules). */
const val DORAI_FIREBASE_WEB_API_KEY = "AIzaSyBoocbXUacJ8fNODJVG63k3Z3Irm9tYco0"
