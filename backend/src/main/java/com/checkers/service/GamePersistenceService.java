package com.checkers.service;

import com.checkers.database.GameDAO;
import com.checkers.database.MoveDAO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Single coordination point for game/replay persistence. */
@Service
public class GamePersistenceService {
    public long createGame(String roomCode,String variantId){return GameDAO.createGame(roomCode,variantId);}
    public boolean registerPlayer(long gameId,long userId,int playerNumber){return GameDAO.registerPlayer(gameId,userId,playerNumber);}
    public boolean saveMove(long gameId,long playerId,int moveNumber,Map<String,Object> move){return MoveDAO.saveMove(gameId,playerId,moveNumber,move);}
    public int getLastMoveNumber(long gameId){return MoveDAO.getLastMoveNumber(gameId);}
    public boolean completeGame(long gameId,Long winnerUserId){return GameDAO.completeGame(gameId,winnerUserId);}
    public boolean isParticipant(long gameId,long userId){return GameDAO.isParticipant(gameId,userId);}
    public JsonNode getGame(long gameId){return GameDAO.getGame(gameId);}
    public ArrayNode getMoves(long gameId){return MoveDAO.getMovesForReplay(gameId);}
}
