package com.checkers.database;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Ordered, validated persistence for individual game moves. */
public final class MoveDAO {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private MoveDAO(){}
    private static HttpRequest.Builder request(String path){
        String key=SupabaseConfig.API_KEY;
        if(key==null||key.isBlank())throw new IllegalStateException("Supabase API key is not configured.");
        return HttpRequest.newBuilder(URI.create(SupabaseConfig.URL+"/rest/v1/"+path)).timeout(Duration.ofSeconds(20))
                .header("apikey",key).header("Authorization","Bearer "+key).header("Accept","application/json");
    }
    public static boolean saveMove(long gameId,long playerId,int moveNumber,String from,String to){
        if(from==null||to==null||from.isBlank()||to.isBlank())return false;
        Map<String,Object> data=Map.of("notation",from+"-"+to,"from",from,"to",to,"capture",false,"king",false);
        return saveMove(gameId,playerId,moveNumber,data);
    }
    public static boolean saveMove(long gameId,long playerId,int moveNumber,Map<String,Object> moveData){
        if(gameId<=0||playerId<=0||moveNumber<=0||moveData==null||moveData.isEmpty())return false;
        try{
            Map<String,Object> entry=new LinkedHashMap<>();entry.put("game_id",gameId);entry.put("player_id",playerId);entry.put("move_number",moveNumber);entry.put("move_data",moveData);
            HttpRequest req=request("game_moves").header("Content-Type","application/json").header("Prefer","return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(entry))).build();
            HttpResponse<String> res=CLIENT.send(req,HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()<200||res.statusCode()>=300){System.err.println("Move save failed game="+gameId+" move="+moveNumber+" player="+playerId+" HTTP="+res.statusCode()+" body="+res.body());return false;}
            return true;
        }catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
         catch(Exception e){System.err.println("Move save exception game="+gameId+" move="+moveNumber+": "+e.getMessage());return false;}
    }
    public static int getLastMoveNumber(long gameId){
        if(gameId<=0)throw new IllegalArgumentException("Invalid game ID.");
        try{
            String path="game_moves?select=move_number&game_id=eq."+gameId+"&order=move_number.desc&limit=1";
            HttpResponse<String> res=CLIENT.send(request(path).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()<200||res.statusCode()>=300)throw new IllegalStateException("Could not retrieve latest move number: HTTP "+res.statusCode()+" "+res.body());
            JsonNode rows=JSON.readTree(res.body());return rows.isArray()&&!rows.isEmpty()?rows.get(0).path("move_number").asInt(0):0;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Move lookup interrupted.");}
         catch(java.io.IOException e){throw new IllegalStateException("Could not parse latest move number.",e);}
    }
    public static ArrayNode getMovesForReplay(long gameId){
        if(gameId<=0)throw new IllegalArgumentException("Invalid game ID.");
        ArrayNode all=JSON.createArrayNode();int last=0;
        try{
            while(true){
                String path="game_moves?game_id=eq."+gameId+"&select=game_id,player_id,move_number,move_data&order=move_number.asc&move_number=gt."+last+"&limit=500";
                HttpResponse<String> res=CLIENT.send(request(path).GET().build(),HttpResponse.BodyHandlers.ofString());
                if(res.statusCode()<200||res.statusCode()>=300)throw new IllegalStateException("Could not retrieve saved moves: HTTP "+res.statusCode()+" "+res.body());
                JsonNode page=JSON.readTree(res.body());if(!page.isArray())throw new IllegalStateException("Invalid replay response.");
                if(page.isEmpty())break;
                for(JsonNode move:page){int number=move.path("move_number").asInt(-1);if(number!=last+1)throw new IllegalStateException("Saved move history has a gap: expected "+(last+1)+" but found "+number+".");all.add(move);last=number;}
                if(page.size()<500)break;
            }
            return all;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Replay retrieval interrupted.");}
         catch(java.io.IOException e){throw new IllegalStateException("Could not parse replay data.",e);}
    }
}
