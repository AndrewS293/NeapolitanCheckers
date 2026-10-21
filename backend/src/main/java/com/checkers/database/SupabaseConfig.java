package com.checkers.database;
// implements supabase configuration for the application
// defines the URL and API key for connecting to the Supabase backend
public class SupabaseConfig {

    //Database configs
    public static final String URL =
        "https://mqsuzuyrkxldbyftjujm.supabase.co"; //need to make this an environment variable

    public static final String API_KEY =
        System.getenv("SUPABASE_SECRET_KEY");
}