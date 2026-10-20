package com.checkers.database;

public class SupabaseConfig {

    //Database configs
    public static final String URL =
        "https://mqsuzuyrkxldbyftjujm.supabase.co"; //need to make this an environment variable

    public static final String API_KEY =
        System.getenv("SUPABASE_SECRET_KEY");
}