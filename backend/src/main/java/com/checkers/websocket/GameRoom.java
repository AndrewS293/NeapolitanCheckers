package com.checkers.websocket;

import org.springframework.web.socket.WebSocketSession;
import com.checkers.logic.GameLogic;

/** In-memory authoritative state for one live room. */
public class GameRoom {
    public enum Status { WAITING, ACTIVE, FINISHED }
    private final String gameId; private final long databaseGameId;
    private final GameVisibility visibility; private final String joinCode; private final String variantId;
    private WebSocketSession player1,player2;
    private String player1Username,player2Username;
    private Long player1UserId,player2UserId;
    private Status status=Status.WAITING;
    private GameLogic gameLogic;
    private int savedMoveCount;

    public GameRoom(String gameId,long databaseGameId,GameVisibility visibility,String joinCode,String variantId,GameLogic gameLogic){
        this.gameId=gameId;this.databaseGameId=databaseGameId;this.visibility=visibility;this.joinCode=joinCode;
        if(variantId==null||variantId.isBlank())throw new IllegalArgumentException("A game variant must be specified.");
        this.variantId=variantId;this.gameLogic=java.util.Objects.requireNonNull(gameLogic,"Game logic cannot be null.");
    }
    public String getGameId(){return gameId;} public long getDatabaseGameId(){return databaseGameId;}
    public String getVariantId(){return variantId;} public GameVisibility getVisibility(){return visibility;} public String getJoinCode(){return joinCode;}
    public synchronized WebSocketSession getPlayer1(){return player1;} public synchronized WebSocketSession getPlayer2(){return player2;}
    public synchronized Status getStatus(){return status;} public synchronized GameLogic getGameLogic(){return gameLogic;}
    public synchronized int getSavedMoveCount(){return savedMoveCount;}
    public synchronized void setSavedMoveCount(int count){if(count<0)throw new IllegalArgumentException("Move count cannot be negative.");savedMoveCount=count;}
    public synchronized void commitSavedMove(GameLogic validatedGame){gameLogic=java.util.Objects.requireNonNull(validatedGame);savedMoveCount++;}
    public synchronized String getPlayer1Username(){return player1Username;} public synchronized String getPlayer2Username(){return player2Username;}
    public synchronized Long getPlayer1UserId(){return player1UserId;} public synchronized Long getPlayer2UserId(){return player2UserId;}
    public synchronized Long getUserId(int player){return player==1?player1UserId:player==2?player2UserId:null;}
    public synchronized void setPlayerUserId(int player,long userId){if(userId<=0)throw new IllegalArgumentException("Invalid user ID.");if(player==1)player1UserId=userId;else if(player==2)player2UserId=userId;else throw new IllegalArgumentException("Player must be 1 or 2.");}

    public synchronized boolean addPlayer(WebSocketSession session,String username){
        if(status==Status.FINISHED||session==null||username==null||username.isBlank())return false;
        if(username.equals(player1Username)){if(player1!=null)return false;player1=session;updateStatus();return true;}
        if(username.equals(player2Username)){if(player2!=null)return false;player2=session;updateStatus();return true;}
        if(player1Username==null){player1=session;player1Username=username;updateStatus();return true;}
        if(player2Username==null){player2=session;player2Username=username;updateStatus();return true;}
        return false;
    }
    public synchronized int disconnectPlayer(WebSocketSession session){
        if(session==null)return 0;
        if(player1!=null&&player1.getId().equals(session.getId())){player1=null;updateStatus();return 1;}
        if(player2!=null&&player2.getId().equals(session.getId())){player2=null;updateStatus();return 2;}
        return 0;
    }
    public synchronized boolean hasDisconnectedPlayer(String username){return username!=null&&((username.equals(player1Username)&&player1==null)||(username.equals(player2Username)&&player2==null));}
    public synchronized boolean isPlayerConnected(int n){return n==1?player1!=null&&player1.isOpen():n==2&&player2!=null&&player2.isOpen();}
    public synchronized String getUsername(int n){return n==1?player1Username:n==2?player2Username:null;}
    public synchronized boolean isFull(){return player1Username!=null&&player2Username!=null;}
    public synchronized boolean isEmpty(){return player1Username==null&&player2Username==null;}
    public synchronized int getPlayerCount(){return (player1!=null&&player1.isOpen()?1:0)+(player2!=null&&player2.isOpen()?1:0);}
    public synchronized int getPlayerNumber(WebSocketSession s){if(s==null)return 0;if(player1!=null&&player1.getId().equals(s.getId()))return 1;if(player2!=null&&player2.getId().equals(s.getId()))return 2;return 0;}
    public synchronized int getPlayerNumberByUsername(String u){if(u!=null&&u.equals(player1Username))return 1;if(u!=null&&u.equals(player2Username))return 2;return 0;}
    public synchronized void finish(){status=Status.FINISHED;}
    private void updateStatus(){if(status==Status.FINISHED)return;status=player1!=null&&player1.isOpen()&&player2!=null&&player2.isOpen()?Status.ACTIVE:Status.WAITING;}
}
